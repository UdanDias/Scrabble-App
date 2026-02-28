package lk.kelaniya.uok.scrabble.scrabbleapp.dao;

import lk.kelaniya.uok.scrabble.scrabbleapp.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameDao extends JpaRepository<GameEntity, String> {
    @Query("SELECT g FROM GameEntity g WHERE g.player1.playerId = :playerId OR g.player2.playerId = :playerId")
    List<GameEntity> getAllGamesByPlayerId(@Param("playerId") String playerId);

    List<GameEntity> findByRound_RoundId(String roundId);

    // In your GameDao or a new query
    @Query("SELECT DISTINCT g.player1.playerId FROM GameEntity g WHERE g.round.tournament.tournamentId = :tournamentId " +
            "UNION SELECT DISTINCT g.player2.playerId FROM GameEntity g WHERE g.round.tournament.tournamentId = :tournamentId")
    List<String> findPlayerIdsByTournamentId(@Param("tournamentId") String tournamentId);
}
