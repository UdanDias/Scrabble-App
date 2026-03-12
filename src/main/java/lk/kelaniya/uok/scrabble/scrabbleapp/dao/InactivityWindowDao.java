package lk.kelaniya.uok.scrabble.scrabbleapp.dao;

import lk.kelaniya.uok.scrabble.scrabbleapp.entity.InactivityWindowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InactivityWindowDao extends JpaRepository<InactivityWindowEntity, String> {

    // All windows for a specific player in a specific tournament
    List<InactivityWindowEntity> findByPlayerIdAndTournamentId(String playerId, String tournamentId);

    // All windows for an entire tournament (used during replay to build the freeze map)
    List<InactivityWindowEntity> findByTournamentId(String tournamentId);

    // Find the currently open window (returnRound is null = still inactive)
    Optional<InactivityWindowEntity> findByPlayerIdAndTournamentIdAndReturnRoundIsNull(
            String playerId, String tournamentId);
}