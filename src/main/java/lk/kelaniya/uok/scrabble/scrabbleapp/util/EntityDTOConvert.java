package lk.kelaniya.uok.scrabble.scrabbleapp.util;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.GameDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PerformanceDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PlayerDTO;
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
        gameDTO.setGameTied(gameEntity.getScore1() == gameEntity.getScore2());
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

}
