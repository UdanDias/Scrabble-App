package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.GameDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.PerformanceDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.PlayerDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.GameDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PerformanceDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.GameEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PerformanceEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PlayerEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.GameNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.InputMarginIncorrectException;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PlayerNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.GameService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.EntityDTOConvert;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.PerformanceCalc;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.UtilData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;



import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional


public class GameServiceImpl implements GameService {

    private final PerformanceCalc performanceCalc;
    private final PerformanceDao performanceDao;
    private final GameDao gameDao;
    private final EntityDTOConvert entityDTOConvert;
    private final PlayerDao playerDao;

    @Override
    public void addGame(GameDTO gameDTO)  {
        gameDTO.setGameId(UtilData.generateGameId());
        int calcMargin= performanceCalc.calcMargin(gameDTO);
        gameDTO.setMargin(calcMargin);
        gameDTO.setWinnerId(performanceCalc.calcWinner(gameDTO));

        gameDTO.setGameTied(gameDTO.getScore1() == gameDTO.getScore2());
        if (gameDTO.isGameTied()) {
            gameDTO.setWinnerId("");
        }

        PlayerEntity player1=playerDao.findById(gameDTO.getPlayer1Id())
                        .orElseThrow(()->new PlayerNotFoundException("Player1 not found"));

        PlayerEntity player2=playerDao.findById(gameDTO.getPlayer2Id())
                .orElseThrow(()->new PlayerNotFoundException("Player2 not found"));

        GameEntity gameEntity=entityDTOConvert.convertGameDTOToGameEntity(gameDTO);
        gameEntity.setPlayer1(player1);
        gameEntity.setPlayer2(player2);

        if (gameDTO.getWinnerId() != null && !gameDTO.getWinnerId().isEmpty()) {
            PlayerEntity winner = playerDao.findById(gameDTO.getWinnerId())
                    .orElseThrow(() -> new PlayerNotFoundException("Winner not found"));
            gameEntity.setWinner(winner);
        }
        gameDao.save(gameEntity);
        System.out.println("from gameServiceImpl DTO : "+gameDTO);
//        System.out.println("from gameServiceImpl DTO : "+gameDTO);


        PerformanceEntity player1Perf=performanceDao.findById(player1.getPlayerId())
                .orElseThrow(()->new PlayerNotFoundException("Player1perf not found"));

        PerformanceEntity player2Perf=performanceDao.findById(player2.getPlayerId())
                .orElseThrow(()->new PlayerNotFoundException("Player2perf not found"));
        System.out.println("from playerServiceImpl EntityPerf1 : "+player1Perf);
        System.out.println("from playerServiceImpl EntityPerf2 : "+player2Perf);




        if (!gameDTO.isGameTied()){
//            PerformanceEntity winner=performanceDao.findById(gameDTO.getWinnerId())
//                    .orElseThrow(()->new PlayerNotFoundException("Player not found"));
            if(gameDTO.getWinnerId().equals(player1.getPlayerId())){
                performanceCalc.updatePerformanceAfterGame(player1Perf,gameDTO);
                player2Perf.setTotalGamesPlayed(player2Perf.getTotalGamesPlayed()+1);

            }else{
                performanceCalc.updatePerformanceAfterGame(player2Perf,gameDTO);
                player1Perf.setTotalGamesPlayed(player1Perf.getTotalGamesPlayed()+1);
            }
        }else{

            performanceCalc.updatePerformanceAfterGame(player1Perf,gameDTO);
            performanceCalc.updatePerformanceAfterGame(player2Perf,gameDTO);
        }
        int cumMargin1= player1Perf.getCumMargin();
        int newCumMargin1=cumMargin1+(gameDTO.getScore1()-gameDTO.getScore2());
        player1Perf.setCumMargin(newCumMargin1);

        int cumMargin2= player2Perf.getCumMargin();
        int newCumMargin2=cumMargin2+(gameDTO.getScore2()-gameDTO.getScore1());
        player2Perf.setCumMargin(newCumMargin2);

        double avgMargin1 = (double) player1Perf.getCumMargin() / player1Perf.getTotalGamesPlayed();
        avgMargin1 = Math.round(avgMargin1 * 100.0) / 100.0;  // round to 2 decimal places
        player1Perf.setAvgMargin(avgMargin1);

        double avgMargin2 = (double) player2Perf.getCumMargin() / player2Perf.getTotalGamesPlayed();
        avgMargin2 = Math.round(avgMargin2 * 100.0) / 100.0;  // round to 2 decimal places
        player2Perf.setAvgMargin(avgMargin2);


        performanceCalc.updateRanks();
    }

    @Override
    public void deleteGame(String gameId) {
        GameEntity gameEntity=gameDao.findById(gameId).orElseThrow(()-> new GameNotFoundException("Game not found"));
        gameDao.delete(gameEntity);
    }

    @Override
    public void updateGame(String gameId, GameDTO gameDTO) {

    }

    @Override
    public GameDTO getSelectedGame(String gameId) {
        return null;
    }

    @Override
    public List<GameDTO> getAllGames() {
        return List.of();
    }
}
