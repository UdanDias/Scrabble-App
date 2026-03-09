package lk.kelaniya.uok.scrabble.scrabbleapp.service;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TournamentPlayerDTO;

import java.util.List;

public interface TournamentPlayerService {

    TournamentPlayerDTO registerPlayer(String tournamentId, String playerId);

    void removePlayer(String tournamentPlayerId);

    List<TournamentPlayerDTO> getPlayersByTournament(String tournamentId);

    /**
     * Marks a round as completed.
     * If the round belongs to "Mini Tournament Uok", applies a -50 margin penalty
     * and +1 gamesPlayed to every registered player who did NOT appear in any game
     * in that round, then recalculates all ranks.
     */
    void completeRound(String roundId);

    /**
     * Checks activity status (ACTIVE/INACTIVE) based on last 3 consecutive rounds.
     * Called after every game mutation.
     */
    void checkAndUpdateInactivePlayersForMiniTournament();
}