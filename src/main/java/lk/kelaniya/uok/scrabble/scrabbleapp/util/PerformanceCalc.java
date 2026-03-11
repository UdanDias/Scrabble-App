package lk.kelaniya.uok.scrabble.scrabbleapp.util;

import lk.kelaniya.uok.scrabble.scrabbleapp.dao.GameDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.PerformanceDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.RoundDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.TournamentPlayerDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.GameDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.GameEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PerformanceEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.RoundEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PlayerNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PerformanceCalc {

    private static final String MINI_TOURNAMENT_NAME = "Mini Tournament Uok";
    private static  final double AbsenceEloDeduct = 20.0;

    private final PerformanceDao performanceDao;
    private final GameDao gameDao;
    private final EntityDTOConvert entityDTOConvert;
    private final TournamentPlayerDao tournamentPlayerDao;
    private final RoundDao roundDao;

    // ── Basic calculations ────────────────────────────────────────────────────

    public String calcWinner(GameDTO gameDTO) {
        String winnerId = "";
        int score1 = gameDTO.getScore1();
        int score2 = gameDTO.getScore2();
        if (score1 > score2) {
            winnerId = gameDTO.getPlayer1Id();
        } else if (score1 < score2) {
            winnerId = gameDTO.getPlayer2Id();
        } else {
            gameDTO.setGameTied(true);
        }
        return winnerId;
    }

    public int calcMargin(GameDTO gameDTO) {
        int score1 = gameDTO.getScore1();
        int score2 = gameDTO.getScore2();
        return score1 > score2 ? score1 - score2 : score2 - score1;
    }

    // ── Performance helpers ───────────────────────────────────────────────────

    public void updatePerformanceAfterGame(PerformanceEntity performanceEntity, GameDTO gameDTO) {
        performanceEntity.setTotalGamesPlayed(performanceEntity.getTotalGamesPlayed() + 1);
        if (!gameDTO.isGameTied()) {
            performanceEntity.setTotalWins(performanceEntity.getTotalWins() + 1);
        } else {
            performanceEntity.setTotalWins(performanceEntity.getTotalWins() + 0.5);
        }
    }

    public void updateBothPlayersPerformance(PerformanceEntity player1Perf,
                                             PerformanceEntity player2Perf,
                                             GameDTO gameDTO) {
        if (!gameDTO.isGameTied()) {
            String winnerId = gameDTO.getWinnerId();
            if (winnerId != null && winnerId.equals(player1Perf.getPlayerId())) {
                updatePerformanceAfterGame(player1Perf, gameDTO);
                player2Perf.setTotalGamesPlayed(player2Perf.getTotalGamesPlayed() + 1);
            } else {
                updatePerformanceAfterGame(player2Perf, gameDTO);
                player1Perf.setTotalGamesPlayed(player1Perf.getTotalGamesPlayed() + 1);
            }
        } else {
            updatePerformanceAfterGame(player1Perf, gameDTO);
            updatePerformanceAfterGame(player2Perf, gameDTO);
        }

        player1Perf.setCumMargin(player1Perf.getCumMargin() + (gameDTO.getScore1() - gameDTO.getScore2()));
        player2Perf.setCumMargin(player2Perf.getCumMargin() + (gameDTO.getScore2() - gameDTO.getScore1()));

        player1Perf.setAvgMargin(Math.round((double) player1Perf.getCumMargin() / player1Perf.getTotalGamesPlayed() * 100.0) / 100.0);
        player2Perf.setAvgMargin(Math.round((double) player2Perf.getCumMargin() / player2Perf.getTotalGamesPlayed() * 100.0) / 100.0);
    }

    public void applyByePerformance(PerformanceEntity performance) {
        performance.setTotalGamesPlayed(performance.getTotalGamesPlayed() + 1);
        performance.setTotalWins(performance.getTotalWins() + 1);
        performance.setCumMargin(performance.getCumMargin() + 50);
        performance.setAvgMargin(
                Math.round((double) performance.getCumMargin() / performance.getTotalGamesPlayed() * 100.0) / 100.0
        );
        performanceDao.save(performance);
    }

    // ── Main recalculation ────────────────────────────────────────────────────

    public void reCalculateAllPerformances() {

        // ── Step 1: Reset all active players ─────────────────────────────────
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

        // ── Step 2: Replay all games ordered by date ──────────────────────────
        List<GameEntity> gamesList = gameDao.findAll()
                .stream()
                .sorted(Comparator.comparing(g -> g.getGameDate() != null
                        ? g.getGameDate() : java.time.LocalDate.MIN))
                .collect(Collectors.toList());

        // Find Mini Tournament id once
        String miniTournamentId = tournamentPlayerDao
                .findByTournamentName(MINI_TOURNAMENT_NAME)
                .stream()
                .findFirst()
                .map(tp -> tp.getTournamentId())
                .orElse(null);

        for (GameEntity game : gamesList) {

            boolean isMiniTournamentGame = miniTournamentId != null
                    && game.getRound() != null
                    && game.getRound().getTournament() != null
                    && miniTournamentId.equals(game.getRound().getTournament().getTournamentId());

            // ── BYE game ──────────────────────────────────────────────────────
            if (game.isBye()) {
                if (game.getPlayer1() == null) continue;

                PerformanceEntity playerPerf = performanceDao.findById(game.getPlayer1().getPlayerId())
                        .orElseThrow(() -> new PlayerNotFoundException("Player not found"));

                // ✅ Elo BEFORE stats update (provisional check uses pre-game count)
                if (isMiniTournamentGame) {
                    double newRating = EloCalculator.calculateBye(
                            playerPerf.getEloRating(),
                            playerPerf.getTotalGamesPlayed()  // ✅ provisional
                    );
                    playerPerf.setEloRating(newRating);
                }

                applyByePerformance(playerPerf); // increments gamesPlayed AFTER Elo
                continue;
            }

            // ── Normal game ───────────────────────────────────────────────────
            if (game.getPlayer1() == null || game.getPlayer2() == null) continue;

            GameDTO gameDTO = entityDTOConvert.convertGameEntityToGameDTO(game);

            PerformanceEntity player1Perf = performanceDao.findById(game.getPlayer1().getPlayerId())
                    .orElseThrow(() -> new PlayerNotFoundException("Player 1 not found"));
            PerformanceEntity player2Perf = performanceDao.findById(game.getPlayer2().getPlayerId())
                    .orElseThrow(() -> new PlayerNotFoundException("Player 2 not found"));

            // ✅ Elo BEFORE stats update (provisional check uses pre-game count)
            if (isMiniTournamentGame) {
                double scoreA = gameDTO.isGameTied() ? 0.5
                        : (gameDTO.getWinnerId() != null
                        && gameDTO.getWinnerId().equals(player1Perf.getPlayerId()) ? 1.0 : 0.0);

                int scoreDiff = Math.abs(gameDTO.getScore1() - gameDTO.getScore2());

                double[] newRatings = EloCalculator.calculate(
                        player1Perf.getEloRating(),
                        player2Perf.getEloRating(),
                        scoreA,
                        scoreDiff,
                        player1Perf.getTotalGamesPlayed(),  // ✅ provisional
                        player2Perf.getTotalGamesPlayed()   // ✅ provisional
                );

                player1Perf.setEloRating(newRatings[0]);
                player2Perf.setEloRating(newRatings[1]);
            }

            // Wins / margin stats AFTER Elo (gamesPlayed incremented here)
            updateBothPlayersPerformance(player1Perf, player2Perf, gameDTO);

            performanceDao.save(player1Perf);
            performanceDao.save(player2Perf);
        }
        System.out.println(">>> miniTournamentId resolved: " + miniTournamentId);
        if (miniTournamentId != null) {
            applyAbsencePenaltiesForMiniTournament(miniTournamentId);
        }
        // ── Step 3: Update ranks ──────────────────────────────────────────────
        updateRanks(miniTournamentId);
    }

    // ── Rank assignment ───────────────────────────────────────────────────────

    public void updateRanks(String miniTournamentId) {

        List<PerformanceEntity> performances = performanceDao.findAllActivePlayers();

        java.util.Set<String> miniTournamentPlayerIds = miniTournamentId != null
                ? tournamentPlayerDao.findByTournamentId(miniTournamentId)
                .stream()
                .map(tp -> tp.getPlayerId())
                .collect(java.util.stream.Collectors.toSet())
                : java.util.Collections.emptySet();

        List<PerformanceEntity> miniWithGames = performances.stream()
                .filter(p -> miniTournamentPlayerIds.contains(p.getPlayerId()))
                .filter(p -> p.getTotalGamesPlayed() != null && p.getTotalGamesPlayed() > 0)
                .collect(Collectors.toList());

        List<PerformanceEntity> othersWithGames = performances.stream()
                .filter(p -> !miniTournamentPlayerIds.contains(p.getPlayerId()))
                .filter(p -> p.getTotalGamesPlayed() != null && p.getTotalGamesPlayed() > 0)
                .collect(Collectors.toList());

        List<PerformanceEntity> noGames = performances.stream()
                .filter(p -> p.getTotalGamesPlayed() == null || p.getTotalGamesPlayed() == 0)
                .collect(Collectors.toList());

        // ── Mini Tournament: sort by Elo DESC ─────────────────────────────────
        miniWithGames.sort((a, b) -> Double.compare(
                b.getEloRating() != null ? b.getEloRating() : EloCalculator.DEFAULT_RATING,
                a.getEloRating() != null ? a.getEloRating() : EloCalculator.DEFAULT_RATING
        ));

        // ── Others: sort by wins DESC, avgMargin DESC ─────────────────────────
        othersWithGames.sort((a, b) -> {
            int cmp = Double.compare(
                    b.getTotalWins() != null ? b.getTotalWins() : 0.0,
                    a.getTotalWins() != null ? a.getTotalWins() : 0.0
            );
            if (cmp != 0) return cmp;
            return Double.compare(
                    b.getAvgMargin() != null ? b.getAvgMargin() : 0.0,
                    a.getAvgMargin() != null ? a.getAvgMargin() : 0.0
            );
        });

        int rank = assignRanksElo(miniWithGames, 1);
        rank = assignRanksWins(othersWithGames, rank);

        for (PerformanceEntity p : noGames) {
            p.setPlayerRank(rank++);
            performanceDao.save(p);
        }
    }

    /** Assigns ranks to a pre-sorted list using Elo as the tiebreak key. */
    private int assignRanksElo(List<PerformanceEntity> sorted, int startRank) {
        int rank = startRank;
        int sameCount = 1;
        PerformanceEntity prev = null;
        for (PerformanceEntity curr : sorted) {
            double currElo = curr.getEloRating() != null ? curr.getEloRating() : EloCalculator.DEFAULT_RATING;
            if (prev != null) {
                double prevElo = prev.getEloRating() != null ? prev.getEloRating() : EloCalculator.DEFAULT_RATING;
                if (Double.compare(currElo, prevElo) != 0) {
                    rank += sameCount;
                    sameCount = 1;
                } else {
                    sameCount++;
                }
            }
            curr.setPlayerRank(rank);
            performanceDao.save(curr);
            prev = curr;
        }
        return rank + (sorted.isEmpty() ? 0 : sameCount);
    }

    /** Assigns ranks to a pre-sorted list using wins+avgMargin as the tiebreak key. */
    private int assignRanksWins(List<PerformanceEntity> sorted, int startRank) {
        int rank = startRank;
        int sameCount = 1;
        PerformanceEntity prev = null;
        for (PerformanceEntity curr : sorted) {
            if (prev != null) {
                double currWins = curr.getTotalWins() != null ? curr.getTotalWins() : 0.0;
                double prevWins = prev.getTotalWins() != null ? prev.getTotalWins() : 0.0;
                double currAvg  = curr.getAvgMargin() != null ? Math.round(curr.getAvgMargin() * 100.0) / 100.0 : 0.0;
                double prevAvg  = prev.getAvgMargin() != null ? Math.round(prev.getAvgMargin() * 100.0) / 100.0 : 0.0;

                if (Double.compare(currWins, prevWins) == 0 && Double.compare(currAvg, prevAvg) == 0) {
                    sameCount++;
                } else {
                    rank += sameCount;
                    sameCount = 1;
                }
            }
            curr.setAvgMargin(curr.getAvgMargin() != null
                    ? Math.round(curr.getAvgMargin() * 100.0) / 100.0 : 0.0);
            curr.setPlayerRank(rank);
            performanceDao.save(curr);
            prev = curr;
        }
        return rank + (sorted.isEmpty() ? 0 : sameCount);
    }
    private void applyAbsencePenaltiesForMiniTournament(String miniTournamentId) {
        System.out.println(">>> applyAbsencePenalties called for tournamentId: " + miniTournamentId);

        List<RoundEntity> completedRounds = roundDao
                .findByTournament_TournamentIdOrderByRoundNumberAsc(miniTournamentId)
                .stream()
                .filter(RoundEntity::isCompleted)
                .collect(Collectors.toList());

        System.out.println(">>> completedRounds count: " + completedRounds.size());

        for (RoundEntity round : completedRounds) {
            List<GameEntity> roundGames = gameDao.findByRound_RoundId(round.getRoundId());

            System.out.println(">>> Round " + round.getRoundNumber() + " has " + roundGames.size() + " games");

            Set<String> playersWhoPlayed = new HashSet<>();
            for (GameEntity game : roundGames) {
                if (game.getPlayer1() != null) playersWhoPlayed.add(game.getPlayer1().getPlayerId());
                if (game.getPlayer2() != null) playersWhoPlayed.add(game.getPlayer2().getPlayerId());
            }

            System.out.println(">>> Players who played: " + playersWhoPlayed);

            tournamentPlayerDao.findByTournamentId(miniTournamentId).forEach(tp -> {
                System.out.println(">>> Checking player: " + tp.getPlayerId() +
                        " registeredFrom=" + tp.getRegisteredFromRoundNumber() +
                        " roundNumber=" + round.getRoundNumber() +
                        " played=" + playersWhoPlayed.contains(tp.getPlayerId()));

                if (tp.getRegisteredFromRoundNumber() <= round.getRoundNumber()
                        && !playersWhoPlayed.contains(tp.getPlayerId())) {

                    performanceDao.findById(tp.getPlayerId()).ifPresent(perf -> {
                        System.out.println(">>> Penalizing: " + tp.getPlayerId() +
                                " from " + perf.getEloRating() + " to " + (perf.getEloRating() - AbsenceEloDeduct));
                        perf.setEloRating(perf.getEloRating() - AbsenceEloDeduct);
                        performanceDao.save(perf);
                    });
                }
            });
        }
    }
}