package lk.kelaniya.uok.scrabble.scrabbleapp.dao;

import lk.kelaniya.uok.scrabble.scrabbleapp.entity.TournamentTeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentTeamDao extends JpaRepository<TournamentTeamEntity, String> {

    @Query("SELECT t FROM TournamentTeamEntity t WHERE t.tournament.tournamentId = :tournamentId")
    List<TournamentTeamEntity> findByTournamentId(@Param("tournamentId") String tournamentId);

    @Query("SELECT t FROM TournamentTeamEntity t WHERE t.tournament.tournamentId = :tournamentId AND t.team.teamId = :teamId")
    Optional<TournamentTeamEntity> findByTournamentIdAndTeamId(
            @Param("tournamentId") String tournamentId,
            @Param("teamId") String teamId);
}