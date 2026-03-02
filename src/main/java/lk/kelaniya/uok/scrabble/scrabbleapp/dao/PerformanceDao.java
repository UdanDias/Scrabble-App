package lk.kelaniya.uok.scrabble.scrabbleapp.dao;

import lk.kelaniya.uok.scrabble.scrabbleapp.entity.GameEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PerformanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PerformanceDao extends JpaRepository<PerformanceEntity, String> {
//    @Query("SELECT p FROM PerformanceEntity p ORDER BY p.playerRank ASC")
//    List<PerformanceEntity> getAllPerformancesOrderedByRank();
@Query("SELECT p FROM PerformanceEntity p JOIN PlayerEntity pl ON p.playerId = pl.playerId WHERE pl.deleted = false ORDER BY p.playerRank ASC")
List<PerformanceEntity> getAllPerformancesOrderedByRank();

    @Query("SELECT p FROM PerformanceEntity p JOIN PlayerEntity pl ON p.playerId = pl.playerId WHERE pl.deleted = false")
    List<PerformanceEntity> findAllActivePlayers();}
