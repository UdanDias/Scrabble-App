package lk.kelaniya.uok.scrabble.scrabbleapp.util;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.*;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.GameEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PerformanceEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PlayerEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EntityDTOConvert {
    private final ModelMapper modelMapper;
    public PlayerEntity convertPlayerDTOToPlayerEntity(PlayerDTO playerDTO) {
        return modelMapper.map(playerDTO,PlayerEntity.class);
    }
    public PlayerDTO convertPlayerEntityToPlayerDTO(PlayerEntity playerEntity) {
        return modelMapper.map(playerEntity,PlayerDTO.class);
    }
    public List<PlayerDTO> convertPlayerEntityListToPlayerDTOList(List<PlayerEntity> playerEntityList) {
        return modelMapper.map(playerEntityList,new TypeToken<List<PlayerDTO>>(){}.getType());
    }

    public GameEntity convertGameDTOToGameEntity(GameDTO gameDTO) {
        return modelMapper.map(gameDTO,GameEntity.class);
    }
    public GameDTO convertGameEntityToGameDTO(GameEntity gameEntity) {
        GameDTO gameDTO = modelMapper.map(gameEntity, GameDTO.class);
        // gameTied is not stored in DB so we derive it from scores
//        gameDTO.setGameTied(gameEntity.getScore1() == gameEntity.getScore2());
        gameDTO.setGameTied(gameEntity.isGameTied());
        // ensure winnerId is null for tied games not empty string
        if (gameDTO.isGameTied()) {
            gameDTO.setWinnerId(null);
        }
        return gameDTO;
    }
    public List<GameDTO> convertGameEntityListToGameDTOList(List<GameEntity> gameEntityList) {
        return gameEntityList.stream()
                .map(this::convertGameEntityToGameDTO)  // changed this line
                .collect(java.util.stream.Collectors.toList());
    }
    public PerformanceEntity convertPerformanceDTOToPerformanceEntity(PerformanceDTO performanceDTO) {
        return modelMapper.map(performanceDTO,PerformanceEntity.class);
    }
    public PerformanceDTO convertPerformanceEntityToPerformanceDTO(PerformanceEntity performanceEntity) {
        return modelMapper.map(performanceEntity,PerformanceDTO.class);
    }
    public List<PerformanceDTO> convertPerformanceEntityListToPerformanceDTOList(List<PerformanceEntity> performanceEntityList) {
        return modelMapper.map(performanceEntityList,new TypeToken<List<PerformanceDTO>>(){}.getType());
    }
    public PlayerGameDTO convertGameEntityToPlayerGameDTO(GameEntity gameEntity) {
        PlayerGameDTO dto = new PlayerGameDTO();

        // Basic fields
        dto.setGameId(gameEntity.getGameId());
        dto.setScore1(gameEntity.getScore1());
        dto.setScore2(gameEntity.getScore2());
        dto.setMargin(gameEntity.getMargin());
        dto.setGameTied(gameEntity.isGameTied());
        dto.setGameDate(gameEntity.getGameDate());
        dto.setBye(gameEntity.isBye());

        // Player 1
        if (gameEntity.getPlayer1() != null) {
            dto.setPlayer1Id(gameEntity.getPlayer1().getPlayerId());
            dto.setPlayer1Name(gameEntity.getPlayer1().getFirstName()
                    + " " + gameEntity.getPlayer1().getLastName());
        }

        // Player 2
        if (gameEntity.getPlayer2() != null) {
            dto.setPlayer2Id(gameEntity.getPlayer2().getPlayerId());
            dto.setPlayer2Name(gameEntity.getPlayer2().getFirstName()
                    + " " + gameEntity.getPlayer2().getLastName());
        } else {
            dto.setPlayer2Name("BYE");
        }

        // Winner
        if (gameEntity.getWinner() != null) {
            dto.setWinnerId(gameEntity.getWinner().getPlayerId());
            dto.setWinnerName(gameEntity.getWinner().getFirstName()
                    + " " + gameEntity.getWinner().getLastName());
        } else {
            dto.setWinnerName(gameEntity.isGameTied() ? "Tied" : "");
        }

        return dto;
    }
    public RankedPlayerDTO convertPerformanceEntityToRankedPlayerDTO(PerformanceEntity performanceEntity) {
        RankedPlayerDTO dto = new RankedPlayerDTO();
        dto.setPlayerId(performanceEntity.getPlayer().getPlayerId());
        dto.setFirstName(performanceEntity.getPlayer().getFirstName());
        dto.setLastName(performanceEntity.getPlayer().getLastName());
        dto.setPlayerRank(performanceEntity.getPlayerRank());
        dto.setTotalWins(performanceEntity.getTotalWins());
        dto.setTotalGamesPlayed(performanceEntity.getTotalGamesPlayed());
        dto.setAvgMargin(performanceEntity.getAvgMargin());
        dto.setCumMargin(performanceEntity.getCumMargin());
        return dto;
    }

}
