package lk.kelaniya.uok.scrabble.scrabbleapp.util;

import lk.kelaniya.uok.scrabble.scrabbleapp.dao.GameDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.PerformanceDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.GameDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PerformanceDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.GameEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PerformanceEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PlayerNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PerformanceCalc {
    private final PerformanceDao performanceDao;
    private final GameDao gameDao;
    private final EntityDTOConvert entityDTOConvert;

    public String calcWinner(GameDTO gameDTO) {
        String winnerId="";
        int score1 = gameDTO.getScore1();
        int score2 = gameDTO.getScore2();

        if (score1 > score2) {
            winnerId=gameDTO.getPlayer1Id();
        }else if (score1 < score2) {
            winnerId=gameDTO.getPlayer2Id();
        }else {
            gameDTO.setGameTied(true);
        }

        return winnerId;
    }
    public int calcMargin(GameDTO gameDTO) {
        int score1 = gameDTO.getScore1();
        int score2 = gameDTO.getScore2();
        int margin;
        if (score1 > score2) {
            margin=score1-score2;
        }else{
            margin=score2-score1;
        }
        return margin;
    }
    public void updatePerformanceAfterGame(PerformanceEntity performanceEntity, GameDTO gameDTO) {
        performanceEntity.setTotalGamesPlayed(performanceEntity.getTotalGamesPlayed()+1);

        if (!gameDTO.isGameTied()){
            performanceEntity.setTotalWins(performanceEntity.getTotalWins()+1);

        }else{
            performanceEntity.setTotalWins(performanceEntity.getTotalWins()+0.5);
        }

    }
//    public void updateRanks() {
//
//        List<PerformanceEntity> performances = performanceDao.findAll();
//
//        // Sort by totalWins DESC, then avgMargin DESC (rounded to 2 decimals)
//        performances.sort((p1, p2) -> {
//            double wins1 = p1.getTotalWins() != null ? p1.getTotalWins() : 0.0;
//            double wins2 = p2.getTotalWins() != null ? p2.getTotalWins() : 0.0;
//
//            int cmpWins = Double.compare(wins2, wins1);
//            if (cmpWins != 0) {
//                return cmpWins;
//            }
//
//            double avg1 = p1.getAvgMargin() != null ? Math.round(p1.getAvgMargin() * 100.0) / 100.0 : 0.0;
//            double avg2 = p2.getAvgMargin() != null ? Math.round(p2.getAvgMargin() * 100.0) / 100.0 : 0.0;
//
//            return Double.compare(avg2, avg1);
//        });
//
//        int rank = 1;
//        int sameRankCount = 1;
//
//        PerformanceEntity previous = null;
//
//        for (PerformanceEntity current : performances) {
//
//            double currentAvg = current.getAvgMargin() != null
//                    ? Math.round(current.getAvgMargin() * 100.0) / 100.0
//                    : 0.0;
//            current.setAvgMargin(currentAvg);
//
//            if (previous != null) {
//                double prevAvg = previous.getAvgMargin() != null
//                        ? Math.round(previous.getAvgMargin() * 100.0) / 100.0
//                        : 0.0;
//
//                double currentWins = current.getTotalWins() != null ? current.getTotalWins() : 0.0;
//                double prevWins = previous.getTotalWins() != null ? previous.getTotalWins() : 0.0;
//
//                // use Double.compare instead of == for wrapper types
//                if (Double.compare(currentWins, prevWins) == 0 &&
//                        Double.compare(currentAvg, prevAvg) == 0) {
//                    current.setPlayerRank(rank);
//                    sameRankCount++;
//                } else {
//                    rank += sameRankCount;
//                    current.setPlayerRank(rank);
//                    sameRankCount = 1;
//                }
//            } else {
//                current.setPlayerRank(rank);
//            }
//            performanceDao.save(current);
//            previous = current;
//        }
//    }
    public void updateRanks() {
        List<PerformanceEntity> performances = performanceDao.findAll();

        // Separate players who have played from those who haven't
        List<PerformanceEntity> playedGames = performances.stream()
                .filter(p -> p.getTotalGamesPlayed() != null && p.getTotalGamesPlayed() > 0)
                .collect(Collectors.toList());

        List<PerformanceEntity> noGames = performances.stream()
                .filter(p -> p.getTotalGamesPlayed() == null || p.getTotalGamesPlayed() == 0)
                .collect(Collectors.toList());

        // Sort only players who have played
        playedGames.sort((p1, p2) -> {
            double wins1 = p1.getTotalWins() != null ? p1.getTotalWins() : 0.0;
            double wins2 = p2.getTotalWins() != null ? p2.getTotalWins() : 0.0;

            int cmpWins = Double.compare(wins2, wins1);
            if (cmpWins != 0) return cmpWins;

            double avg1 = p1.getAvgMargin() != null ? Math.round(p1.getAvgMargin() * 100.0) / 100.0 : 0.0;
            double avg2 = p2.getAvgMargin() != null ? Math.round(p2.getAvgMargin() * 100.0) / 100.0 : 0.0;
            return Double.compare(avg2, avg1);
        });

        // Assign ranks to players who have played
        int rank = 1;
        int sameRankCount = 1;
        PerformanceEntity previous = null;

        for (PerformanceEntity current : playedGames) {
            double currentAvg = current.getAvgMargin() != null
                    ? Math.round(current.getAvgMargin() * 100.0) / 100.0 : 0.0;
            current.setAvgMargin(currentAvg);

            if (previous != null) {
                double prevAvg = previous.getAvgMargin() != null
                        ? Math.round(previous.getAvgMargin() * 100.0) / 100.0 : 0.0;
                double currentWins = current.getTotalWins() != null ? current.getTotalWins() : 0.0;
                double prevWins = previous.getTotalWins() != null ? previous.getTotalWins() : 0.0;

                if (Double.compare(currentWins, prevWins) == 0 &&
                        Double.compare(currentAvg, prevAvg) == 0) {
                    current.setPlayerRank(rank);
                    sameRankCount++;
                } else {
                    rank += sameRankCount;
                    current.setPlayerRank(rank);
                    sameRankCount = 1;
                }
            } else {
                current.setPlayerRank(rank);
            }
            performanceDao.save(current);
            previous = current;
        }

        // Assign remaining ranks to players with no games — they always come last
        int lastRank = rank + sameRankCount;
        for (PerformanceEntity noGamePlayer : noGames) {
            noGamePlayer.setPlayerRank(lastRank);
            performanceDao.save(noGamePlayer);
            lastRank++;
        }
    }
    public void updateBothPlayersPerformance(PerformanceEntity player1Perf, PerformanceEntity player2Perf, GameDTO gameDTO) {

        if (!gameDTO.isGameTied()) {
            // add null check before calling .equals()
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

        // Update cumulative margins
        player1Perf.setCumMargin(player1Perf.getCumMargin() + (gameDTO.getScore1() - gameDTO.getScore2()));
        player2Perf.setCumMargin(player2Perf.getCumMargin() + (gameDTO.getScore2() - gameDTO.getScore1()));

        // Update average margins
        player1Perf.setAvgMargin(Math.round((double) player1Perf.getCumMargin() / player1Perf.getTotalGamesPlayed() * 100.0) / 100.0);
        player2Perf.setAvgMargin(Math.round((double) player2Perf.getCumMargin() / player2Perf.getTotalGamesPlayed() * 100.0) / 100.0);
    }

//    public void reCalculateAllPerformances(){
//        List<PerformanceEntity> performanceEntityList = performanceDao.findAll();
//        for(PerformanceEntity performance : performanceEntityList){
//            performance.setTotalGamesPlayed(0);
//            performance.setAvgMargin(0.0);
//            performance.setPlayerRank(0);
//            performance.setCumMargin(0);
//            performance.setTotalWins(0.0);
//            performanceDao.save(performance);
//        }
//
//        List<GameEntity> gamesList = gameDao.findAll();
//        for (GameEntity game : gamesList) {
//            if (game.getPlayer1() == null || game.getPlayer2() == null) {
//                continue;
//            }
//            GameDTO gameDTO = entityDTOConvert.convertGameEntityToGameDTO(game);
//            PerformanceEntity player1Perf = performanceDao.findById(game.getPlayer1().getPlayerId())
//                    .orElseThrow(() -> new PlayerNotFoundException("Player 1 not found"));
//            PerformanceEntity player2Perf = performanceDao.findById(game.getPlayer2().getPlayerId())
//                    .orElseThrow(() -> new PlayerNotFoundException("Player 2 not found"));
//            updateBothPlayersPerformance(player1Perf, player2Perf, gameDTO);
//        }
//        updateRanks();
//    }

    public void reCalculateAllPerformances() {
        // reset all performances
        List<PerformanceEntity> performanceEntityList = performanceDao.findAll();
        for (PerformanceEntity performance : performanceEntityList) {
            performance.setTotalGamesPlayed(0);
            performance.setAvgMargin(0.0);
            performance.setPlayerRank(0);
            performance.setCumMargin(0);
            performance.setTotalWins(0.0);
            performanceDao.save(performance);
        }

        List<GameEntity> gamesList = gameDao.findAll();
        for (GameEntity game : gamesList) {

            // ✅ bye game — handle separately and skip normal logic completely
            if (game.isBye()) {
                PerformanceEntity playerPerf = performanceDao.findById(game.getPlayer1().getPlayerId())
                        .orElseThrow(() -> new PlayerNotFoundException("Player not found"));
                applyByePerformance(playerPerf); // +1 game, +1 win, +50 margin
                continue; // ← skips everything below, no score difference calculated
            }

            // ✅ null check for safety on normal games
            if (game.getPlayer1() == null || game.getPlayer2() == null) continue;

            GameDTO gameDTO = entityDTOConvert.convertGameEntityToGameDTO(game);
            PerformanceEntity player1Perf = performanceDao.findById(game.getPlayer1().getPlayerId())
                    .orElseThrow(() -> new PlayerNotFoundException("Player 1 not found"));
            PerformanceEntity player2Perf = performanceDao.findById(game.getPlayer2().getPlayerId())
                    .orElseThrow(() -> new PlayerNotFoundException("Player 2 not found"));

            updateBothPlayersPerformance(player1Perf, player2Perf, gameDTO);
        }
        updateRanks();
    }
    public void applyByePerformance(PerformanceEntity performance) {
        performance.setTotalGamesPlayed(performance.getTotalGamesPlayed() + 1);
        performance.setTotalWins(performance.getTotalWins() + 1);
        performance.setCumMargin(performance.getCumMargin() + 50); // ✅ fixed +50, not score difference
        performance.setAvgMargin(
                Math.round((double) performance.getCumMargin() / performance.getTotalGamesPlayed() * 100.0) / 100.0
        );
        performanceDao.save(performance);
    }



}
