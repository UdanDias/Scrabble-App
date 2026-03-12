package lk.kelaniya.uok.scrabble.scrabbleapp.dao;

import lk.kelaniya.uok.scrabble.scrabbleapp.entity.TournamentPlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TournamentPlayerDao extends JpaRepository<TournamentPlayerEntity, String> {

    // ── Use explicit JPQL since the FK is now a relationship, not a plain String ──

    @Query("SELECT tp FROM TournamentPlayerEntity tp WHERE tp.tournament.tournamentId = :tournamentId")
    List<TournamentPlayerEntity> findByTournamentId(@Param("tournamentId") String tournamentId);

    @Query("SELECT tp FROM TournamentPlayerEntity tp WHERE tp.tournament.tournamentId = :tournamentId AND tp.playerId = :playerId")
    Optional<TournamentPlayerEntity> findByTournamentIdAndPlayerId(
            @Param("tournamentId") String tournamentId,
            @Param("playerId") String playerId);

    @Query("SELECT tp FROM TournamentPlayerEntity tp WHERE tp.tournamentName = :tournamentName")
    List<TournamentPlayerEntity> findByTournamentName(@Param("tournamentName") String tournamentName);
}