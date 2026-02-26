package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.GameDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.PerformanceDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.PlayerDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.RoundDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.GameDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PerformanceDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PlayerGameDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.GameEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PerformanceEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PlayerEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.RoundEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.GameNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.InputMarginIncorrectException;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PlayerNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.RoundNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.GameService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.EntityDTOConvert;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.PerformanceCalc;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.UtilData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional


public class GameServiceImpl implements GameService {

    private final PerformanceCalc performanceCalc;
    private final GameDao gameDao;
    private final EntityDTOConvert entityDTOConvert;
    private final PlayerDao playerDao;
    private final RoundDao roundDao;

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
        // inside addGame, before gameDao.save(gameEntity):
        if (gameDTO.getRoundId() != null && !gameDTO.getRoundId().isEmpty()) {
            RoundEntity round = roundDao.findById(gameDTO.getRoundId())
                    .orElseThrow(() -> new RoundNotFoundException("Round not found"));
            gameEntity.setRound(round);
        }
        gameDao.save(gameEntity);
        performanceCalc.reCalculateAllPerformances();
    }

    @Override
    public void addByeGame(String playerId, LocalDate gameDate, String roundId) {
        PlayerEntity player = playerDao.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found"));

        GameEntity gameEntity = new GameEntity();
        gameEntity.setGameId(UtilData.generateGameId());
        gameEntity.setPlayer1(player);
        gameEntity.setPlayer2(null);
        gameEntity.setScore1(0);
        gameEntity.setScore2(0);
        gameEntity.setMargin(50);
        gameEntity.setWinner(player);
        gameEntity.setGameDate(gameDate);
        gameEntity.setBye(true);
        gameEntity.setGameTied(false);

        // ← add round link
        if (roundId != null && !roundId.isEmpty()) {
            RoundEntity round = roundDao.findById(roundId)
                    .orElseThrow(() -> new RoundNotFoundException("Round not found"));
            gameEntity.setRound(round);
        }

        gameDao.save(gameEntity);
        performanceCalc.reCalculateAllPerformances();
    }

    @Override
    public void deleteGame(String gameId) {
        GameEntity gameEntity=gameDao.findById(gameId).orElseThrow(()-> new GameNotFoundException("Game not found"));
        gameDao.delete(gameEntity);
        performanceCalc.reCalculateAllPerformances();
    }

    @Override
    public GameDTO updateGame(String gameId, GameDTO gameDTO) {
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
            gameEntity.setWinner(null);
        }
        gameEntity.setGameDate(gameDTO.getGameDate());

        // add this before gameDao.save(gameEntity) in updateGame
        if (gameDTO.getRoundId() != null && !gameDTO.getRoundId().isEmpty()) {
            RoundEntity round = roundDao.findById(gameDTO.getRoundId())
                    .orElseThrow(() -> new RoundNotFoundException("Round not found"));
            gameEntity.setRound(round);
        } else {
            gameEntity.setRound(null); // allow removing a game from a round
        }
        GameEntity updatedGame = gameDao.save(gameEntity);
        performanceCalc.reCalculateAllPerformances();
        return entityDTOConvert.convertGameEntityToGameDTO(updatedGame);
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
    @Override
    public List<PlayerGameDTO> getAllGamesByPlayerId(String playerId) {
        playerDao.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found"));

        List<GameEntity> games = gameDao.getAllGamesByPlayerId(playerId);

        // ✅ Use new conversion method
        return games.stream()
                .map(entityDTOConvert::convertGameEntityToPlayerGameDTO)
                .collect(Collectors.toList());
    }
    public List<PlayerGameDTO> getGamesByRound(String roundId) {
        List<GameEntity> games = gameDao.findByRound_RoundId(roundId);
        return games.stream()
                .map(entityDTOConvert::convertGameEntityToPlayerGameDTO)
                .collect(Collectors.toList());
    }
}
