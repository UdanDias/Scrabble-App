package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.GameDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.PerformanceDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PairingDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PerformanceDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PlayerDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.RankedPlayerDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.GameEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PerformanceEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PerformanceNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.PerformanceService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.EntityDTOConvert;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.UtilData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PerformanceServiceImpl implements PerformanceService {
    private final PerformanceDao performanceDao;
    private final EntityDTOConvert entityDTOConvert;
    private final GameDao gameDao;

    @Override
    public PerformanceDTO getSelectedPerformance(String playerId) {
        PerformanceEntity performance=performanceDao.findById(playerId)
                .orElseThrow(()->new PerformanceNotFoundException("performance not found"));
        return entityDTOConvert.convertPerformanceEntityToPerformanceDTO(performance);
    }

    @Override
    public List<PerformanceDTO> getAllPerformances() {
        return entityDTOConvert.convertPerformanceEntityListToPerformanceDTOList(performanceDao.findAll());
    }

//    @Override
//    public List<RankedPlayerDTO> getPlayersOrderedByRank() {
//        List<PerformanceEntity> performances = performanceDao.getAllPerformancesOrderedByRank();
//        return performances.stream()
//                .map(entityDTOConvert::convertPerformanceEntityToRankedPlayerDTO)
//                .collect(Collectors.toList());
//    }
        @Override
        public List<RankedPlayerDTO> getPlayersOrderedByRank() {
            List<PerformanceEntity> performances = performanceDao.getAllPerformancesOrderedByRank();
            return performances.stream()
                    .filter(p -> p.getPlayer() != null)
                    .filter(p -> p.getTotalGamesPlayed() != null && p.getTotalGamesPlayed() > 0)
                    .map(entityDTOConvert::convertPerformanceEntityToRankedPlayerDTO)
                    .collect(Collectors.toList());
        }
//    public List<RankedPlayerDTO> getPlayersOrderedByRankByTournament(String tournamentId) {
//        List<PerformanceEntity> performances = performanceDao.getAllPerformancesOrderedByRank();
//
//        // Get all player IDs who played in this tournament
//        List<String> playerIdsInTournament = gameDao.findPlayerIdsByTournamentId(tournamentId);
//
//        return performances.stream()
//                .filter(p -> playerIdsInTournament.contains(p.getPlayerId()))
//                .map(entityDTOConvert::convertPerformanceEntityToRankedPlayerDTO)
//                .collect(Collectors.toList());
//    }

    public List<RankedPlayerDTO> getPlayersOrderedByRankByTournament(String tournamentId) {
        // Get only games from this tournament
        List<GameEntity> tournamentGames = gameDao.getGamesByTournamentId(tournamentId);

        if (tournamentGames.isEmpty()) {
            return List.of();
        }

        // Collect unique player IDs from tournament games
        Map<String, TournamentPlayerStats> statsMap = new HashMap<>();

        for (GameEntity game : tournamentGames) {
            if (game.isBye()) {
                if (game.getPlayer1() == null) continue;
                String pid = game.getPlayer1().getPlayerId();
                TournamentPlayerStats stats = statsMap.getOrDefault(pid, new TournamentPlayerStats(pid, game.getPlayer1().getFirstName(), game.getPlayer1().getLastName()));
                stats.gamesPlayed++;
                stats.wins++;
                stats.cumMargin += 50;
                statsMap.put(pid, stats);
            } else {
                if (game.getPlayer1() == null || game.getPlayer2() == null) continue;

                String p1id = game.getPlayer1().getPlayerId();
                String p2id = game.getPlayer2().getPlayerId();

                TournamentPlayerStats p1stats = statsMap.getOrDefault(p1id, new TournamentPlayerStats(p1id, game.getPlayer1().getFirstName(), game.getPlayer1().getLastName()));
                TournamentPlayerStats p2stats = statsMap.getOrDefault(p2id, new TournamentPlayerStats(p2id, game.getPlayer2().getFirstName(), game.getPlayer2().getLastName()));

                p1stats.gamesPlayed++;
                p2stats.gamesPlayed++;
                p1stats.cumMargin += (game.getScore1() - game.getScore2());
                p2stats.cumMargin += (game.getScore2() - game.getScore1());

                if (!game.isGameTied() && game.getWinner() != null) {
                    if (game.getWinner().getPlayerId().equals(p1id)) p1stats.wins++;
                    else p2stats.wins++;
                }

                statsMap.put(p1id, p1stats);
                statsMap.put(p2id, p2stats);
            }
        }

        // Calculate avg margin and assign ranks
        List<TournamentPlayerStats> statsList = new ArrayList<>(statsMap.values());
        statsList.forEach(s -> s.avgMargin = s.gamesPlayed > 0 ? Math.round((double) s.cumMargin / s.gamesPlayed * 100.0) / 100.0 : 0.0);

        // Sort by wins DESC then avgMargin DESC
        statsList.sort((a, b) -> {
            int cmp = Double.compare(b.wins, a.wins);
            if (cmp != 0) return cmp;
            return Double.compare(b.avgMargin, a.avgMargin);
        });

        // Assign ranks
        int rank = 1;
        for (int i = 0; i < statsList.size(); i++) {
            if (i > 0) {
                TournamentPlayerStats prev = statsList.get(i - 1);
                TournamentPlayerStats curr = statsList.get(i);
                if (Double.compare(curr.wins, prev.wins) != 0 || Double.compare(curr.avgMargin, prev.avgMargin) != 0) {
                    rank = i + 1;
                }
            }
            statsList.get(i).rank = rank;
        }

        // Convert to DTOs
        return statsList.stream().map(s -> {
            RankedPlayerDTO dto = new RankedPlayerDTO();
            dto.setPlayerId(s.playerId);
            dto.setFirstName(s.firstName);
            dto.setLastName(s.lastName);
            dto.setPlayerRank(s.rank);
            dto.setTotalWins(s.wins);
            dto.setTotalGamesPlayed(s.gamesPlayed);
            dto.setCumMargin(s.cumMargin);
            dto.setAvgMargin(s.avgMargin);
            return dto;
        }).collect(Collectors.toList());
    }

    // Inner helper class
    private static class TournamentPlayerStats {
        String playerId, firstName, lastName;
        int gamesPlayed = 0, cumMargin = 0, rank = 1;
        double wins = 0, avgMargin = 0;

        TournamentPlayerStats(String playerId, String firstName, String lastName) {
            this.playerId = playerId;
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    @Override
    public List<PairingDTO> getSwissPairings(String tournamentId) {

        // 1. Get players ordered by rank for this tournament (reuse existing method)
        List<RankedPlayerDTO> rankedPlayers = getPlayersOrderedByRankByTournament(tournamentId);

        if (rankedPlayers.isEmpty()) {
            return List.of();
        }

        // 2. Group players by their totalWins value (exact value as key)
        //    LinkedHashMap preserves insertion order; we insert highest-wins group first
        //    because rankedPlayers is already sorted rank ASC (most wins first).
        LinkedHashMap<Double, List<RankedPlayerDTO>> groupMap = new LinkedHashMap<>();
        for (RankedPlayerDTO player : rankedPlayers) {
            double key = player.getTotalWins();
            groupMap.computeIfAbsent(key, k -> new ArrayList<>()).add(player);
        }

        // 3. Build an ordered list of groups (highest wins → lowest wins)
        List<List<RankedPlayerDTO>> groups = new ArrayList<>(groupMap.values());

        // 4. Cascade: if a group is odd-sized, take the TOP player of the group
        //    BELOW and move them UP into the odd group above.
        //    Work top-to-bottom so each fixed group stays even for subsequent passes.
        for (int i = 0; i < groups.size() - 1; i++) {
            if (groups.get(i).size() % 2 != 0) {
                List<RankedPlayerDTO> currentGroup = groups.get(i);
                List<RankedPlayerDTO> nextGroup    = groups.get(i + 1);

                if (!nextGroup.isEmpty()) {
                    // Remove the top player of the next (lower) group …
                    RankedPlayerDTO promoted = nextGroup.remove(0);
                    // … and add them to the bottom of the current (upper) group
                    currentGroup.add(promoted);
                }
            }
        }

        // 5. Remove any groups that became empty after promotion
        groups.removeIf(List::isEmpty);

        // 6. Handle final group: if still odd, the LAST player gets a BYE
        RankedPlayerDTO byePlayer = null;
        List<RankedPlayerDTO> lastGroup = groups.get(groups.size() - 1);
        if (lastGroup.size() % 2 != 0) {
            byePlayer = lastGroup.remove(lastGroup.size() - 1);
        }

        // 7. Generate pairings for each group:
        //    Split each group in half; top-half[i] faces bottom-half[i]
        List<PairingDTO> pairings = new ArrayList<>();
        int boardNumber = 1;

        for (int g = 0; g < groups.size(); g++) {
            List<RankedPlayerDTO> group = groups.get(g);
            if (group.isEmpty()) continue;

            int half = group.size() / 2;
            List<RankedPlayerDTO> topHalf    = group.subList(0, half);
            List<RankedPlayerDTO> bottomHalf = group.subList(half, group.size());

            for (int i = 0; i < half; i++) {
                RankedPlayerDTO p1 = topHalf.get(i);
                RankedPlayerDTO p2 = bottomHalf.get(i);

                PairingDTO pairing = new PairingDTO();
                pairing.setBoardNumber(boardNumber++);
                pairing.setGroupNumber(g + 1);
                pairing.setBye(false);

                pairing.setPlayer1Id(p1.getPlayerId());
                pairing.setPlayer1Name(p1.getFirstName() + " " + p1.getLastName());
                pairing.setPlayer1Wins(p1.getTotalWins());
                pairing.setPlayer1Rank(p1.getPlayerRank());

                pairing.setPlayer2Id(p2.getPlayerId());
                pairing.setPlayer2Name(p2.getFirstName() + " " + p2.getLastName());
                pairing.setPlayer2Wins(p2.getTotalWins());
                pairing.setPlayer2Rank(p2.getPlayerRank());

                pairings.add(pairing);
            }
        }

        // 8. Append BYE entry at the end
        if (byePlayer != null) {
            PairingDTO byePairing = new PairingDTO();
            byePairing.setBoardNumber(boardNumber);
            byePairing.setGroupNumber(groups.size() + 1);
            byePairing.setBye(true);

            byePairing.setPlayer1Id(byePlayer.getPlayerId());
            byePairing.setPlayer1Name(byePlayer.getFirstName() + " " + byePlayer.getLastName());
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
}
