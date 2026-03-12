package lk.kelaniya.uok.scrabble.scrabbleapp.util;

import lk.kelaniya.uok.scrabble.scrabbleapp.dao.*;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.GameDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.PlayerActivityStatus;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.*;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PlayerNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PerformanceCalc {

    private static final String MINI_TOURNAMENT_NAME = "Mini Tournament Uok";
    private static final double ABSENCE_ELO_DEDUCT   = 20.0;

    private final PerformanceDao      performanceDao;
    private final GameDao             gameDao;
    private final EntityDTOConvert    entityDTOConvert;
    private final TournamentPlayerDao tournamentPlayerDao;
    private final RoundDao            roundDao;
    private final InactivityWindowDao inactivityWindowDao;

    // ── Basic calculations ────────────────────────────────────────────────────

    public String calcWinner(GameDTO gameDTO) {
        int score1 = gameDTO.getScore1();
        int score2 = gameDTO.getScore2();
        if (score1 > score2)      return gameDTO.getPlayer1Id();
        else if (score1 < score2) return gameDTO.getPlayer2Id();
        else { gameDTO.setGameTied(true); return ""; }
    }

    public int calcMargin(GameDTO gameDTO) {
        int score1 = gameDTO.getScore1();
        int score2 = gameDTO.getScore2();
        return score1 > score2 ? score1 - score2 : score2 - score1;
    }

    // ── Performance helpers ───────────────────────────────────────────────────

    public void updatePerformanceAfterGame(PerformanceEntity perf, GameDTO gameDTO) {
        perf.setTotalGamesPlayed(perf.getTotalGamesPlayed() + 1);
        perf.setTotalWins(perf.getTotalWins() + (gameDTO.isGameTied() ? 0.5 : 1.0));
    }

    public void updateBothPlayersPerformance(PerformanceEntity p1Perf,
                                             PerformanceEntity p2Perf,
                                             GameDTO gameDTO) {
        if (!gameDTO.isGameTied()) {
            String winnerId = gameDTO.getWinnerId();
            if (winnerId != null && winnerId.equals(p1Perf.getPlayerId())) {
                updatePerformanceAfterGame(p1Perf, gameDTO);
                p2Perf.setTotalGamesPlayed(p2Perf.getTotalGamesPlayed() + 1);
            } else {
                updatePerformanceAfterGame(p2Perf, gameDTO);
                p1Perf.setTotalGamesPlayed(p1Perf.getTotalGamesPlayed() + 1);
            }
        } else {
            updatePerformanceAfterGame(p1Perf, gameDTO);
            updatePerformanceAfterGame(p2Perf, gameDTO);
        }

        p1Perf.setCumMargin(p1Perf.getCumMargin() + (gameDTO.getScore1() - gameDTO.getScore2()));
        p2Perf.setCumMargin(p2Perf.getCumMargin() + (gameDTO.getScore2() - gameDTO.getScore1()));

        p1Perf.setAvgMargin(Math.round((double) p1Perf.getCumMargin() / p1Perf.getTotalGamesPlayed() * 100.0) / 100.0);
        p2Perf.setAvgMargin(Math.round((double) p2Perf.getCumMargin() / p2Perf.getTotalGamesPlayed() * 100.0) / 100.0);
    }

    public void applyByePerformance(PerformanceEntity perf) {
        perf.setTotalGamesPlayed(perf.getTotalGamesPlayed() + 1);
        perf.setTotalWins(perf.getTotalWins() + 1);
        perf.setCumMargin(perf.getCumMargin() + 50);
        perf.setAvgMargin(
                Math.round((double) perf.getCumMargin() / perf.getTotalGamesPlayed() * 100.0) / 100.0
        );
        performanceDao.save(perf);
    }

    // ── Main recalculation ────────────────────────────────────────────────────

    public void reCalculateAllPerformances() {

        // ── Step 1: Reset all performances ───────────────────────────────────
        List<PerformanceEntity> allPerformances = performanceDao.findAllActivePlayers();
        for (PerformanceEntity perf : allPerformances) {
            perf.setTotalGamesPlayed(0);
            perf.setAvgMargin(0.0);
            perf.setPlayerRank(0);
            perf.setCumMargin(0);
            perf.setTotalWins(0.0);
            perf.setEloRating(EloCalculator.DEFAULT_RATING);
            performanceDao.save(perf);
        }

        // ── Step 2: Resolve mini tournament ──────────────────────────────────
        String miniTournamentId = tournamentPlayerDao
                .findByTournamentName(MINI_TOURNAMENT_NAME)
                .stream()
                .findFirst()
                .map(tp -> tp.getTournament().getTournamentId())
                .orElse(null);

        // ── Step 3: Build freeze windows map ─────────────────────────────────
        // For each player, a list of [fromRound, returnRound) windows.
        // During replay, if a game's round falls inside any window → skip Elo update.
        // returnRound = null means the window is still open (player currently inactive).
        //
        // Example: [(3, 9), (12, null)]
        //   → frozen for rounds 3-8
        //   → active for rounds 9-11
        //   → frozen from round 12 onward
        Map<String, List<InactivityWindowEntity>> freezeWindows = new HashMap<>();
        if (miniTournamentId != null) {
            List<InactivityWindowEntity> allWindows =
                    inactivityWindowDao.findByTournamentId(miniTournamentId);
            for (InactivityWindowEntity w : allWindows) {
                freezeWindows.computeIfAbsent(w.getPlayerId(), k -> new ArrayList<>()).add(w);
            }
        }

        // ── Step 4: Replay all games ordered by date ──────────────────────────
        List<GameEntity> gamesList = gameDao.findAll()
                .stream()
                .sorted(Comparator.comparing(g -> g.getGameDate() != null
                        ? g.getGameDate() : java.time.LocalDate.MIN))
                .collect(Collectors.toList());

        for (GameEntity game : gamesList) {

            boolean isMiniTournamentGame = miniTournamentId != null
                    && game.getRound() != null
                    && game.getRound().getTournament() != null
                    && miniTournamentId.equals(game.getRound().getTournament().getTournamentId());

            int roundNumber = (game.getRound() != null) ? game.getRound().getRoundNumber() : 0;

            // ── BYE game ──────────────────────────────────────────────────────
            if (game.isBye()) {
                if (game.getPlayer1() == null) continue;
                String pid = game.getPlayer1().getPlayerId();

                PerformanceEntity playerPerf = performanceDao.findById(pid)
                        .orElseThrow(() -> new PlayerNotFoundException("Player not found"));

                // ✅ Only update Elo if the player is NOT frozen in this round
                if (isMiniTournamentGame && !isFrozen(pid, roundNumber, freezeWindows)) {
                    double newRating = EloCalculator.calculateBye(
                            playerPerf.getEloRating(),
                            playerPerf.getTotalGamesPlayed()
                    );
                    playerPerf.setEloRating(newRating);
                }

                applyByePerformance(playerPerf);
                continue;
            }

            // ── Normal game ───────────────────────────────────────────────────
            if (game.getPlayer1() == null || game.getPlayer2() == null) continue;

            String p1id = game.getPlayer1().getPlayerId();
            String p2id = game.getPlayer2().getPlayerId();

            GameDTO gameDTO = entityDTOConvert.convertGameEntityToGameDTO(game);

            PerformanceEntity p1Perf = performanceDao.findById(p1id)
                    .orElseThrow(() -> new PlayerNotFoundException("Player 1 not found"));
            PerformanceEntity p2Perf = performanceDao.findById(p2id)
                    .orElseThrow(() -> new PlayerNotFoundException("Player 2 not found"));

            // ✅ Only update Elo if NEITHER player is frozen in this round
            boolean p1Frozen = isMiniTournamentGame && isFrozen(p1id, roundNumber, freezeWindows);
            boolean p2Frozen = isMiniTournamentGame && isFrozen(p2id, roundNumber, freezeWindows);

            if (isMiniTournamentGame && !p1Frozen && !p2Frozen) {
                double scoreA = gameDTO.isGameTied() ? 0.5
                        : (gameDTO.getWinnerId() != null
                        && gameDTO.getWinnerId().equals(p1id) ? 1.0 : 0.0);

                int scoreDiff = Math.abs(gameDTO.getScore1() - gameDTO.getScore2());

                double[] newRatings = EloCalculator.calculate(
                        p1Perf.getEloRating(), p2Perf.getEloRating(),
                        scoreA, scoreDiff,
                        p1Perf.getTotalGamesPlayed(),
                        p2Perf.getTotalGamesPlayed()
                );
                p1Perf.setEloRating(newRatings[0]);
                p2Perf.setEloRating(newRatings[1]);
            }

            updateBothPlayersPerformance(p1Perf, p2Perf, gameDTO);
            performanceDao.save(p1Perf);
            performanceDao.save(p2Perf);
        }

        // ── Step 5: Apply absence penalties ──────────────────────────────────
        if (miniTournamentId != null) {
            applyAbsencePenaltiesForMiniTournament(miniTournamentId, freezeWindows);
        }

        // ── Step 6: Update ranks ──────────────────────────────────────────────
        updateRanks(miniTournamentId);
    }

    // ── Absence penalties ─────────────────────────────────────────────────────

    /**
     * For every completed round, penalise active players who didn't play.
     * A player is skipped for a round if that round falls inside any of their
     * freeze windows — meaning they were inactive at that time.
     *
     * Freeze window rule: fromRound <= roundNumber < returnRound
     * (returnRound = null means the window is still open, i.e. frozen forever from fromRound)
     */
    private void applyAbsencePenaltiesForMiniTournament(
            String miniTournamentId,
            Map<String, List<InactivityWindowEntity>> freezeWindows) {

        List<RoundEntity> completedRounds = roundDao
                .findByTournament_TournamentIdOrderByRoundNumberAsc(miniTournamentId)
                .stream()
                .filter(RoundEntity::isCompleted)
                .collect(Collectors.toList());

        for (RoundEntity round : completedRounds) {
            int roundNumber = round.getRoundNumber();
            List<GameEntity> roundGames = gameDao.findByRound_RoundId(round.getRoundId());

            Set<String> playersWhoPlayed = new HashSet<>();
            for (GameEntity game : roundGames) {
                if (game.getPlayer1() != null) playersWhoPlayed.add(game.getPlayer1().getPlayerId());
                if (game.getPlayer2() != null) playersWhoPlayed.add(game.getPlayer2().getPlayerId());
            }

            tournamentPlayerDao.findByTournamentId(miniTournamentId).forEach(tp -> {
                String pid = tp.getPlayerId();

                // ✅ Skip penalty if this round is inside any of the player's freeze windows
                if (isFrozen(pid, roundNumber, freezeWindows)) return;

                if (tp.getRegisteredFromRoundNumber() <= roundNumber
                        && !playersWhoPlayed.contains(pid)) {
                    performanceDao.findById(pid).ifPresent(perf -> {
                        perf.setEloRating(perf.getEloRating() - ABSENCE_ELO_DEDUCT);
                        performanceDao.save(perf);
                    });
                }
            });
        }
    }

    // ── Freeze window check ───────────────────────────────────────────────────

    /**
     * Returns true if the given roundNumber falls inside any of the player's
     * inactivity windows.
     *
     * Window covers: fromRound <= roundNumber < returnRound
     * If returnRound is null → window is still open → frozen from fromRound onward
     */
    private boolean isFrozen(String playerId,
                             int roundNumber,
                             Map<String, List<InactivityWindowEntity>> freezeWindows) {
        List<InactivityWindowEntity> windows = freezeWindows.get(playerId);
        if (windows == null || windows.isEmpty()) return false;

        for (InactivityWindowEntity w : windows) {
            boolean afterStart  = roundNumber >= w.getFromRound();
            boolean beforeEnd   = w.getReturnRound() == null || roundNumber < w.getReturnRound();
            if (afterStart && beforeEnd) return true;
        }
        return false;
    }

    // ── Rank assignment ───────────────────────────────────────────────────────

    public void updateRanks(String miniTournamentId) {
        List<PerformanceEntity> performances = performanceDao.findAllActivePlayers();

        Set<String> miniPlayerIds = miniTournamentId != null
                ? tournamentPlayerDao.findByTournamentId(miniTournamentId)
                .stream().map(TournamentPlayerEntity::getPlayerId).collect(Collectors.toSet())
                : Collections.emptySet();

        List<PerformanceEntity> miniWithGames = performances.stream()
                .filter(p -> miniPlayerIds.contains(p.getPlayerId()))
                .filter(p -> p.getTotalGamesPlayed() != null && p.getTotalGamesPlayed() > 0)
                .collect(Collectors.toList());

        List<PerformanceEntity> othersWithGames = performances.stream()
                .filter(p -> !miniPlayerIds.contains(p.getPlayerId()))
                .filter(p -> p.getTotalGamesPlayed() != null && p.getTotalGamesPlayed() > 0)
                .collect(Collectors.toList());

        List<PerformanceEntity> noGames = performances.stream()
                .filter(p -> p.getTotalGamesPlayed() == null || p.getTotalGamesPlayed() == 0)
                .collect(Collectors.toList());

        miniWithGames.sort((a, b) -> Double.compare(
                b.getEloRating() != null ? b.getEloRating() : EloCalculator.DEFAULT_RATING,
                a.getEloRating() != null ? a.getEloRating() : EloCalculator.DEFAULT_RATING));

        othersWithGames.sort((a, b) -> {
            int cmp = Double.compare(
                    b.getTotalWins() != null ? b.getTotalWins() : 0.0,
                    a.getTotalWins() != null ? a.getTotalWins() : 0.0);
            if (cmp != 0) return cmp;
            return Double.compare(
                    b.getAvgMargin() != null ? b.getAvgMargin() : 0.0,
                    a.getAvgMargin() != null ? a.getAvgMargin() : 0.0);
        });

        int rank = assignRanksElo(miniWithGames, 1);
        rank = assignRanksWins(othersWithGames, rank);
        for (PerformanceEntity p : noGames) {
            p.setPlayerRank(rank++);
            performanceDao.save(p);
        }
    }

    private int assignRanksElo(List<PerformanceEntity> sorted, int startRank) {
        int rank = startRank, sameCount = 1;
        PerformanceEntity prev = null;
        for (PerformanceEntity curr : sorted) {
            double currElo = curr.getEloRating() != null ? curr.getEloRating() : EloCalculator.DEFAULT_RATING;
            if (prev != null) {
                double prevElo = prev.getEloRating() != null ? prev.getEloRating() : EloCalculator.DEFAULT_RATING;
                if (Double.compare(currElo, prevElo) != 0) { rank += sameCount; sameCount = 1; }
                else sameCount++;
            }
            curr.setPlayerRank(rank);
            performanceDao.save(curr);
            prev = curr;
        }
        return rank + (sorted.isEmpty() ? 0 : sameCount);
    }

    private int assignRanksWins(List<PerformanceEntity> sorted, int startRank) {
        int rank = startRank, sameCount = 1;
        PerformanceEntity prev = null;
        for (PerformanceEntity curr : sorted) {
            if (prev != null) {
                double cW = curr.getTotalWins() != null ? curr.getTotalWins() : 0.0;
                double pW = prev.getTotalWins() != null ? prev.getTotalWins() : 0.0;
                double cA = curr.getAvgMargin() != null ? Math.round(curr.getAvgMargin() * 100.0) / 100.0 : 0.0;
                double pA = prev.getAvgMargin() != null ? Math.round(prev.getAvgMargin() * 100.0) / 100.0 : 0.0;
                if (Double.compare(cW, pW) == 0 && Double.compare(cA, pA) == 0) sameCount++;
                else { rank += sameCount; sameCount = 1; }
            }
            curr.setAvgMargin(curr.getAvgMargin() != null
                    ? Math.round(curr.getAvgMargin() * 100.0) / 100.0 : 0.0);
            curr.setPlayerRank(rank);
            performanceDao.save(curr);
            prev = curr;
        }
        return rank + (sorted.isEmpty() ? 0 : sameCount);
    }
}