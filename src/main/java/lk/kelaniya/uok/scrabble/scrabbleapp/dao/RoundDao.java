package lk.kelaniya.uok.scrabble.scrabbleapp.dao;

import lk.kelaniya.uok.scrabble.scrabbleapp.entity.RoundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoundDao extends JpaRepository<RoundEntity, String> {

    List<RoundEntity> findByTournament_TournamentId(String tournamentId);

    // Used by inactivity check and penalty logic
    List<RoundEntity> findByTournament_TournamentIdOrderByRoundNumberAsc(String tournamentId);
}