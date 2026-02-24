package lk.kelaniya.uok.scrabble.scrabbleapp.dao;

import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PerformanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PerformanceDao extends JpaRepository<PerformanceEntity, String> {
    @Query("SELECT p FROM PerformanceEntity p ORDER BY p.playerRank ASC")
    List<PerformanceEntity> getAllPerformancesOrderedByRank();
}
