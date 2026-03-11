
package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.*;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PairingDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PerformanceDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.RankedPlayerDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.GameEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PerformanceEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.RoundEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.TournamentPlayerEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PerformanceNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.PerformanceService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.EloCalculator;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.EntityDTOConvert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class PerformanceServiceImpl implements PerformanceService {

    private static final String MINI_TOURNAMENT_NAME = "Mini Tournament Uok";

    private final PerformanceDao  performanceDao;
    private final EntityDTOConvert entityDTOConvert;
    private final GameDao          gameDao;
    private final RoundDao         roundDao;
    private final TournamentPlayerDao tournamentPlayerDao;

    // ── Basic queries ─────────────────────────────────────────────────────────

    @Override
    public PerformanceDTO getSelectedPerformance(String playerId) {
        PerformanceEntity performance = performanceDao.findById(playerId)
                .orElseThrow(() -> new PerformanceNotFoundException("performance not found"));
        return entityDTOConvert.convertPerformanceEntityToPerformanceDTO(performance);
    }

    @Override
    public List<PerformanceDTO> getAllPerformances() {
        return entityDTOConvert.convertPerformanceEntityListToPerformanceDTOList(performanceDao.findAll());
    }

    /**
     * Global leaderboard removed — frontend now requires a tournament to be selected.
     * Kept for interface compatibility only.
     */
    @Override
    public List<RankedPlayerDTO> getPlayersOrderedByRank() {
        return List.of();
    }

    // ── Tournament leaderboard ────────────────────────────────────────────────

    /**
     * Returns ranked players for a specific tournament.
     *
     * - Mini Tournament Uok → Elo-based ranking (each player starts at 1200, games replayed chronologically)
     * - All other tournaments → wins + avgMargin ranking
     */
    public List<RankedPlayerDTO> getPlayersOrderedByRankByTournament(String tournamentId) {
        List<GameEntity> tournamentGames = gameDao.getGamesByTournamentId(tournamentId);
        if (tournamentGames.isEmpty()) return List.of();

        // ── Detect Mini Tournament ────────────────────────────────────────────
        boolean isMiniTournament = tournamentGames.stream()
                .filter(g -> g.getRound() != null && g.getRound().getTournament() != null)
                .anyMatch(g -> MINI_TOURNAMENT_NAME.equals(
                        g.getRound().getTournament().getTournamentName()));

        if (!isMiniTournament) {
            // ── Non-Mini: simple wins+margin replay (unchanged) ───────────────
            return replayNonMiniTournament(tournamentGames);
        }

        // ── Mini Tournament: round-by-round Elo replay ────────────────────────
        //
        // We replay games grouped by round, in round-number order.
        // After each COMPLETED round we snapshot every player's Elo.
        // previousEloRating = snapshot taken after the second-to-last completed round
        //                     (i.e. the Elo they had before the most-recent completed round).
        // This means the OLD column always shows the pre-last-round Elo regardless of
        // whether the user is currently viewing mid-round or post-round.

        // Collect all completed rounds for this tournament, ordered by round number
        List<RoundEntity> allRounds = roundDao.findByTournament_TournamentId(tournamentId)
                .stream()
                .sorted(Comparator.comparingInt(RoundEntity::getRoundNumber))
                .collect(Collectors.toList());

        List<RoundEntity> completedRounds = allRounds.stream()
                .filter(RoundEntity::isCompleted)
                .collect(Collectors.toList());

        // Group all tournament games by roundId
        Map<String, List<GameEntity>> gamesByRound = new HashMap<>();
        for (GameEntity game : tournamentGames) {
            if (game.getRound() == null) continue;
            gamesByRound.computeIfAbsent(game.getRound().getRoundId(), k -> new ArrayList<>()).add(game);
        }

        Map<String, TournamentPlayerStats> statsMap = new HashMap<>();

        // Snapshot of Elo after the second-to-last completed round (= OLD column)
        Map<String, Double> previousEloSnapshot = new HashMap<>(); // playerId → elo
        // Snapshot after the last completed round (= NEW column)
        Map<String, Double> lastCompletedEloSnapshot = new HashMap<>();

        // ── Replay round by round ──────────────────────────────────────────────
        for (int i = 0; i < allRounds.size(); i++) {
            RoundEntity round = allRounds.get(i);
            List<GameEntity> roundGames = gamesByRound.getOrDefault(round.getRoundId(), List.of());

            // Sort games within the round by date
            roundGames = roundGames.stream()
                    .sorted(Comparator.comparing(g -> g.getGameDate() != null
                            ? g.getGameDate() : java.time.LocalDate.MIN))
                    .collect(Collectors.toList());

            for (GameEntity game : roundGames) {
                processGame(game, statsMap, true);
            }

            // After processing this round, if it's completed, rotate snapshots
            // ✅ New code
            if (round.isCompleted()) {
                // Apply absence penalty before snapshotting
                List<TournamentPlayerEntity> registrations =
                        tournamentPlayerDao.findByTournamentId(tournamentId);
                Set<String> playersWhoPlayed = gamesByRound
                        .getOrDefault(round.getRoundId(), List.of())
                        .stream()
                        .flatMap(g -> Stream.of(
                                g.getPlayer1() != null ? g.getPlayer1().getPlayerId() : null,
                                g.getPlayer2() != null ? g.getPlayer2().getPlayerId() : null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                for (TournamentPlayerEntity tp : registrations) {
                    if (tp.getRegisteredFromRoundNumber() <= round.getRoundNumber()
                            && !playersWhoPlayed.contains(tp.getPlayerId())) {
                        TournamentPlayerStats stats = statsMap.get(tp.getPlayerId());
                        if (stats != null) {
                            stats.eloRating -= EloCalculator.ABSENCE_PENALTY;
                        }
                    }
                }

                // Rotate snapshots AFTER penalty is applied
                previousEloSnapshot = new HashMap<>(lastCompletedEloSnapshot);
                for (Map.Entry<String, TournamentPlayerStats> entry : statsMap.entrySet()) {
                    lastCompletedEloSnapshot.put(entry.getKey(), entry.getValue().eloRating);
                }
            }
        }

        // ── Build DTOs ────────────────────────────────────────────────────────
        List<TournamentPlayerStats> statsList = new ArrayList<>(statsMap.values());
        statsList.forEach(s -> s.avgMargin = s.gamesPlayed > 0
                ? Math.round((double) s.cumMargin / s.gamesPlayed * 100.0) / 100.0 : 0.0);

        statsList.sort((a, b) -> Double.compare(b.eloRating, a.eloRating));

        int rank = 1;
        for (int i = 0; i < statsList.size(); i++) {
            if (i > 0 && Double.compare(statsList.get(i).eloRating,
                    statsList.get(i - 1).eloRating) != 0) {
                rank = i + 1;
            }
            statsList.get(i).rank = rank;
        }

        final Map<String, Double> prevEloFinal = previousEloSnapshot;
        final Map<String, Double> lastEloFinal = lastCompletedEloSnapshot;

        return statsList.stream().map(s -> {
            RankedPlayerDTO dto = new RankedPlayerDTO();
            dto.setPlayerId(s.playerId);
            dto.setFirstName(s.firstName);
            dto.setLastName(s.lastName);
            dto.setUsername(s.username);
            dto.setPlayerRank(s.rank);
            dto.setTotalWins(s.wins);
            dto.setTotalGamesPlayed(s.gamesPlayed);
            dto.setCumMargin(s.cumMargin);
            dto.setAvgMargin(s.avgMargin);
            // NEW = Elo after last completed round (stable; not affected by in-progress games)
            dto.setEloRating(lastEloFinal.getOrDefault(s.playerId, EloCalculator.DEFAULT_RATING));
            // OLD = Elo before the last completed round started
            dto.setPreviousEloRating(prevEloFinal.getOrDefault(s.playerId, EloCalculator.DEFAULT_RATING));
            dto.setProvisional(EloCalculator.isProvisional(s.gamesPlayed));
            return dto;
        }).collect(Collectors.toList());
    }

    /** Non-Mini Tournament: simple wins+margin replay, no Elo. */
    private List<RankedPlayerDTO> replayNonMiniTournament(List<GameEntity> games) {
        Map<String, TournamentPlayerStats> statsMap = new HashMap<>();
        games.stream()
                .sorted(Comparator.comparing(g -> g.getGameDate() != null
                        ? g.getGameDate() : java.time.LocalDate.MIN))
                .forEach(game -> processGame(game, statsMap, false));

        List<TournamentPlayerStats> statsList = new ArrayList<>(statsMap.values());
        statsList.forEach(s -> s.avgMargin = s.gamesPlayed > 0
                ? Math.round((double) s.cumMargin / s.gamesPlayed * 100.0) / 100.0 : 0.0);
        statsList.sort((a, b) -> {
            int cmp = Double.compare(b.wins, a.wins);
            return cmp != 0 ? cmp : Double.compare(b.avgMargin, a.avgMargin);
        });

        int rank = 1;
        for (int i = 0; i < statsList.size(); i++) {
            if (i > 0) {
                TournamentPlayerStats prev = statsList.get(i - 1);
                TournamentPlayerStats curr = statsList.get(i);
                if (Double.compare(curr.wins, prev.wins) != 0
                        || Double.compare(curr.avgMargin, prev.avgMargin) != 0) {
                    rank = i + 1;
                }
            }
            statsList.get(i).rank = rank;
        }

        return statsList.stream().map(s -> {
            RankedPlayerDTO dto = new RankedPlayerDTO();
            dto.setPlayerId(s.playerId);
            dto.setFirstName(s.firstName);
            dto.setLastName(s.lastName);
            dto.setUsername(s.username);
            dto.setPlayerRank(s.rank);
            dto.setTotalWins(s.wins);
            dto.setTotalGamesPlayed(s.gamesPlayed);
            dto.setCumMargin(s.cumMargin);
            dto.setAvgMargin(s.avgMargin);
            dto.setEloRating(null);
            dto.setPreviousEloRating(null);
            dto.setProvisional(null);
            return dto;
        }).collect(Collectors.toList());
    }

    /** Processes a single game into statsMap. Updates Elo only if isMiniTournament=true. */
    private void processGame(GameEntity game,
                             Map<String, TournamentPlayerStats> statsMap,
                             boolean isMiniTournament) {
        if (game.isBye()) {
            if (game.getPlayer1() == null) return;
            String pid = game.getPlayer1().getPlayerId();
            TournamentPlayerStats stats = statsMap.computeIfAbsent(pid,
                    id -> new TournamentPlayerStats(id,
                            game.getPlayer1().getFirstName(),
                            game.getPlayer1().getLastName(),
                            game.getPlayer1().getUsername()));
            stats.gamesPlayed++;
            stats.wins++;
            stats.cumMargin += 50;
            if (isMiniTournament) {
                stats.eloRating = EloCalculator.calculateBye(stats.eloRating, stats.gamesPlayed - 1);
            }
        } else {
            if (game.getPlayer1() == null || game.getPlayer2() == null) return;
            String p1id = game.getPlayer1().getPlayerId();
            String p2id = game.getPlayer2().getPlayerId();
            TournamentPlayerStats p1 = statsMap.computeIfAbsent(p1id,
                    id -> new TournamentPlayerStats(id,
                            game.getPlayer1().getFirstName(),
                            game.getPlayer1().getLastName(),
                            game.getPlayer1().getUsername()));
            TournamentPlayerStats p2 = statsMap.computeIfAbsent(p2id,
                    id -> new TournamentPlayerStats(id,
                            game.getPlayer2().getFirstName(),
                            game.getPlayer2().getLastName(),
                            game.getPlayer2().getUsername()));
            p1.gamesPlayed++;
            p2.gamesPlayed++;
            boolean tied   = game.isGameTied();
            boolean p1wins = !tied && game.getWinner() != null
                    && game.getWinner().getPlayerId().equals(p1id);
            if (!tied) { if (p1wins) p1.wins++; else p2.wins++; }
            else       { p1.wins += 0.5; p2.wins += 0.5; }
            p1.cumMargin += (game.getScore1() - game.getScore2());
            p2.cumMargin += (game.getScore2() - game.getScore1());
            if (isMiniTournament) {
                double scoreA = tied ? 0.5 : (p1wins ? 1.0 : 0.0);
//                double[] newRatings = EloCalculator.calculate(p1.eloRating, p2.eloRating, scoreA);
                int scoreDiff = Math.abs(game.getScore1() - game.getScore2());
                double[] newRatings = EloCalculator.calculate(
                        p1.eloRating, p2.eloRating,
                        scoreA, scoreDiff,
                        p1.gamesPlayed - 1,   // pre-game count (already incremented above)
                        p2.gamesPlayed - 1
                );
                p1.eloRating = newRatings[0];
                p2.eloRating = newRatings[1];
            }
        }
    }

    // ── Swiss pairings ────────────────────────────────────────────────────────

    @Override
    public List<PairingDTO> getSwissPairings(String tournamentId) {

        List<RankedPlayerDTO> rankedPlayers = getPlayersOrderedByRankByTournament(tournamentId);
        if (rankedPlayers.isEmpty()) return List.of();

        LinkedHashMap<Double, List<RankedPlayerDTO>> groupMap = new LinkedHashMap<>();
        for (RankedPlayerDTO player : rankedPlayers) {
            groupMap.computeIfAbsent(player.getTotalWins(), k -> new ArrayList<>()).add(player);
        }

        List<List<RankedPlayerDTO>> groups = new ArrayList<>(groupMap.values());

        for (int i = 0; i < groups.size() - 1; i++) {
            if (groups.get(i).size() % 2 != 0) {
                List<RankedPlayerDTO> next = groups.get(i + 1);
                if (!next.isEmpty()) groups.get(i).add(next.remove(0));
            }
        }
        groups.removeIf(List::isEmpty);

        RankedPlayerDTO byePlayer = null;
        List<RankedPlayerDTO> lastGroup = groups.get(groups.size() - 1);
        if (lastGroup.size() % 2 != 0) {
            byePlayer = lastGroup.remove(lastGroup.size() - 1);
        }

        List<PairingDTO> pairings = new ArrayList<>();
        int boardNumber = 1;

        for (int g = 0; g < groups.size(); g++) {
            List<RankedPlayerDTO> group = groups.get(g);
            if (group.isEmpty()) continue;
            int half = group.size() / 2;

            for (int i = 0; i < half; i++) {
                RankedPlayerDTO p1 = group.get(i);
                RankedPlayerDTO p2 = group.get(half + i);

                PairingDTO pairing = new PairingDTO();
                pairing.setBoardNumber(boardNumber++);
                pairing.setGroupNumber(g + 1);
                pairing.setBye(false);
                pairing.setPlayer1Id(p1.getPlayerId());
                pairing.setPlayer1Name(p1.getUsername());
                pairing.setPlayer1Wins(p1.getTotalWins());
                pairing.setPlayer1Rank(p1.getPlayerRank());
                pairing.setPlayer2Id(p2.getPlayerId());
                pairing.setPlayer2Name(p2.getUsername());
                pairing.setPlayer2Wins(p2.getTotalWins());
                pairing.setPlayer2Rank(p2.getPlayerRank());
                pairings.add(pairing);
            }
        }

        if (byePlayer != null) {
            PairingDTO byePairing = new PairingDTO();
            byePairing.setBoardNumber(boardNumber);
            byePairing.setGroupNumber(groups.size() + 1);
            byePairing.setBye(true);
            byePairing.setPlayer1Id(byePlayer.getPlayerId());
            byePairing.setPlayer1Name(byePlayer.getUsername());
            byePairing.setPlayer1Wins(byePlayer.getTotalWins());
            byePairing.setPlayer1Rank(byePlayer.getPlayerRank());
            byePairing.setPlayer2Id(null);
            byePairing.setPlayer2Name(null);
            byePairing.setPlayer2Wins(-1);
            byePairing.setPlayer2Rank(-1);
            pairings.add(byePairing);
        }

        return pairings;
    }

    // ── Inner stats helper ────────────────────────────────────────────────────

    private static class TournamentPlayerStats {
        String playerId, firstName, lastName, username;
        int    gamesPlayed = 0, cumMargin = 0, rank = 1;
        double wins = 0, avgMargin = 0;
        double eloRating = EloCalculator.DEFAULT_RATING; // starts at 1200 per tournament scope

        TournamentPlayerStats(String playerId, String firstName, String lastName, String username) {
            this.playerId  = playerId;
            this.firstName = firstName;
            this.lastName  = lastName;
            this.username  = username;
        }
    }
}