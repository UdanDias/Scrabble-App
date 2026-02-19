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

        performanceCalc.reCalculateAllPerformances();
    }

    @Override
    public void deleteGame(String gameId) {
        GameEntity gameEntity=gameDao.findById(gameId).orElseThrow(()-> new GameNotFoundException("Game not found"));
        gameDao.delete(gameEntity);
        performanceCalc.reCalculateAllPerformances();
    }

    @Override
    public void updateGame(String gameId, GameDTO gameDTO) {
        GameEntity gameEntity=gameDao.findById(gameId).orElseThrow(()->new GameNotFoundException("Game not found"));
        PlayerEntity player1=playerDao.findById(gameDTO.getPlayer1Id())
                        .orElseThrow(()->new PlayerNotFoundException("Player1 Not Found"));
        PlayerEntity player2=playerDao.findById(gameDTO.getPlayer2Id())
                        .orElseThrow(()->new PlayerNotFoundException("Player2 Not Found"));
        gameEntity.setPlayer1(player1);
        gameEntity.setPlayer2(player2);
        gameEntity.setScore1(gameDTO.getScore1());
        gameEntity.setScore2(gameDTO.getScore2());
        gameEntity.setMargin(performanceCalc.calcMargin(gameDTO));

        String winnerId= performanceCalc.calcWinner(gameDTO);
        if(winnerId.equals(player1.getPlayerId())){
            gameEntity.setWinner(player1);
        }else if(winnerId.equals(player2.getPlayerId())){
            gameEntity.setWinner(player2);
        }else{
            gameEntity.setWinner(null);//tied game has no winner
        }
        gameEntity.setGameDate(gameDTO.getGameDate());
        gameDao.save(gameEntity);


        performanceCalc.reCalculateAllPerformances();

    }

    @Override
    public GameDTO getSelectedGame(String gameId) {
        GameEntity gameEntity=gameDao.findById(gameId).orElseThrow(()->new GameNotFoundException("Game not found"));
        return entityDTOConvert.convertGameEntityToGameDTO(gameEntity);
    }

    @Override
    public List<GameDTO> getAllGames() {
        return entityDTOConvert.convertGameEntityListToGameDTOList(gameDao.findAll());
    }
}
