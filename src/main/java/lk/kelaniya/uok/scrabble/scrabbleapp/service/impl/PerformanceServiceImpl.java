package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.GameDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.PerformanceDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.TournamentPlayerDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PairingDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PerformanceDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.RankedPlayerDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.PlayerActivityStatus;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.GameEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PerformanceEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.TournamentPlayerEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PerformanceNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.PerformanceService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.EloCalculator;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.EntityDTOConvert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PerformanceServiceImpl implements PerformanceService {

    private static final String MINI_TOURNAMENT_NAME = "Mini Tournament Uok";

    private final PerformanceDao performanceDao;
    private final EntityDTOConvert entityDTOConvert;
    private final GameDao gameDao;
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

    @Override
    public List<RankedPlayerDTO> getPlayersOrderedByRank() {
        return List.of();
    }

    // ── Tournament leaderboard ────────────────────────────────────────────────

    public List<RankedPlayerDTO> getPlayersOrderedByRankByTournament(String tournamentId) {
        List<GameEntity> tournamentGames = gameDao.getGamesByTournamentId(tournamentId);

        if (tournamentGames.isEmpty()) {
            return List.of();
        }

        // Detect Mini Tournament by name
        boolean isMiniTournament = tournamentGames.stream()
                .filter(g -> g.getRound() != null && g.getRound().getTournament() != null)
                .anyMatch(g -> MINI_TOURNAMENT_NAME.equals(
                        g.getRound().getTournament().getTournamentName()));

        // Fetch inactive player ids for this tournament
        Set<String> inactivePlayerIds = tournamentPlayerDao.findByTournamentId(tournamentId)
                .stream()
                .filter(tp -> tp.getActivityStatus() == PlayerActivityStatus.INACTIVE)
                .map(TournamentPlayerEntity::getPlayerId)
                .collect(Collectors.toSet());

        // Sort games chronologically for correct Elo replay
        List<GameEntity> sortedGames = tournamentGames.stream()
                .sorted(Comparator.comparing(g -> g.getGameDate() != null
                        ? g.getGameDate() : java.time.LocalDate.MIN))
                .collect(Collectors.toList());

        Map<String, TournamentPlayerStats> statsMap = new HashMap<>();

        for (GameEntity game : sortedGames) {

            if (game.isBye()) {
                if (game.getPlayer1() == null) continue;
                String pid = game.getPlayer1().getPlayerId();
                if (inactivePlayerIds.contains(pid)) continue;

                TournamentPlayerStats stats = statsMap.computeIfAbsent(pid,
                        id -> new TournamentPlayerStats(id,
                                game.getPlayer1().getFirstName(),
                                game.getPlayer1().getLastName(),
                                game.getPlayer1().getUsername()));

                // ✅ Elo BEFORE incrementing gamesPlayed (provisional check uses pre-game count)
                if (isMiniTournament) {
                    stats.previousEloRating = stats.eloRating;
                    stats.eloRating = EloCalculator.calculateBye(stats.eloRating, stats.gamesPlayed);
                }

                stats.gamesPlayed++;
                stats.wins++;
                stats.cumMargin += game.getMargin() > 0 ? game.getMargin() : 50;

            } else {
                if (game.getPlayer1() == null || game.getPlayer2() == null) continue;

                String p1id = game.getPlayer1().getPlayerId();
                String p2id = game.getPlayer2().getPlayerId();

                if (inactivePlayerIds.contains(p1id) || inactivePlayerIds.contains(p2id)) continue;

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

                boolean tied   = game.isGameTied();
                boolean p1wins = !tied && game.getWinner() != null
                        && game.getWinner().getPlayerId().equals(p1id);

                // ✅ Elo BEFORE incrementing gamesPlayed (provisional check uses pre-game count)
                if (isMiniTournament) {
                    double scoreA = tied ? 0.5 : (p1wins ? 1.0 : 0.0);
                    int scoreDiff = Math.abs(game.getScore1() - game.getScore2());
                    p1.previousEloRating = p1.eloRating; // ✅ snapshot before
                    p2.previousEloRating = p2.eloRating;
                    double[] newRatings = EloCalculator.calculate(
                            p1.eloRating,
                            p2.eloRating,
                            scoreA,
                            scoreDiff,
                            p1.gamesPlayed,  // ✅ provisional check
                            p2.gamesPlayed   // ✅ provisional check
                    );
                    p1.eloRating = newRatings[0];
                    p2.eloRating = newRatings[1];
                }

                p1.gamesPlayed++;
                p2.gamesPlayed++;

                if (!tied) {
                    if (p1wins) p1.wins++; else p2.wins++;
                } else {
                    p1.wins += 0.5;
                    p2.wins += 0.5;
                }

                p1.cumMargin += (game.getScore1() - game.getScore2());
                p2.cumMargin += (game.getScore2() - game.getScore1());
            }
        }

        // Calculate avgMargin
        List<TournamentPlayerStats> statsList = new ArrayList<>(statsMap.values());
        statsList.forEach(s -> s.avgMargin = s.gamesPlayed > 0
                ? Math.round((double) s.cumMargin / s.gamesPlayed * 100.0) / 100.0 : 0.0);

        // ── Sort ──────────────────────────────────────────────────────────────
        if (isMiniTournament) {
            statsList.sort((a, b) -> Double.compare(b.eloRating, a.eloRating));
        } else {
            statsList.sort((a, b) -> {
                int cmp = Double.compare(b.wins, a.wins);
                if (cmp != 0) return cmp;
                return Double.compare(b.avgMargin, a.avgMargin);
            });
        }

        // ── Assign ranks with tie support ─────────────────────────────────────
        int rank = 1;
        for (int i = 0; i < statsList.size(); i++) {
            if (i > 0) {
                TournamentPlayerStats prev = statsList.get(i - 1);
                TournamentPlayerStats curr = statsList.get(i);
                boolean newRank = isMiniTournament
                        ? Double.compare(curr.eloRating, prev.eloRating) != 0
                        : (Double.compare(curr.wins, prev.wins) != 0
                        || Double.compare(curr.avgMargin, prev.avgMargin) != 0);
                if (newRank) rank = i + 1;
            }
            statsList.get(i).rank = rank;
        }

        // ── Map to DTOs ───────────────────────────────────────────────────────
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
            dto.setEloRating(isMiniTournament ? s.eloRating : null);
            dto.setProvisional(isMiniTournament && EloCalculator.isProvisional(s.gamesPlayed));
            dto.setPreviousEloRating(isMiniTournament ? s.previousEloRating : null);// ✅
            return dto;
        }).collect(Collectors.toList());
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
        double eloRating = EloCalculator.DEFAULT_RATING;
        double previousEloRating = EloCalculator.DEFAULT_RATING;

        TournamentPlayerStats(String playerId, String firstName, String lastName, String username) {
            this.playerId  = playerId;
            this.firstName = firstName;
            this.lastName  = lastName;
            this.username  = username;
        }
    }
}