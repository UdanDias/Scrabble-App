package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.GameDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.PerformanceDao;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
}
