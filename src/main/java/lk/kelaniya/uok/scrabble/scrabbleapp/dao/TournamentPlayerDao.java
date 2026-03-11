package lk.kelaniya.uok.scrabble.scrabbleapp.dao;

import lk.kelaniya.uok.scrabble.scrabbleapp.entity.TournamentPlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentPlayerDao extends JpaRepository<TournamentPlayerEntity, String> {

    // All registrations for a given tournament
    List<TournamentPlayerEntity> findByTournamentId(String tournamentId);

    // All tournaments a player is registered in
    List<TournamentPlayerEntity> findByPlayerId(String playerId);

    // Check for duplicate registration before saving
    Optional<TournamentPlayerEntity> findByTournamentIdAndPlayerId(String tournamentId, String playerId);

    // Used by inactivity check — get all players in a specific tournament by name
    @Query("SELECT tp FROM TournamentPlayerEntity tp WHERE tp.tournamentName = :name")
    List<TournamentPlayerEntity> findByTournamentName(@Param("name") String tournamentName);
}