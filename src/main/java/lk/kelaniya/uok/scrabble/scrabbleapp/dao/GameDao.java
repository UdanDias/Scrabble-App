package lk.kelaniya.uok.scrabble.scrabbleapp.dao;

import lk.kelaniya.uok.scrabble.scrabbleapp.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameDao extends JpaRepository<GameEntity, String> {
    List<GameEntity> findByWinner_PlayerId(String playerId);
    List<GameEntity> findByPlayer1_PlayerId(String playerId);
    List<GameEntity> findByPlayer2_PlayerId(String playerId);
}
