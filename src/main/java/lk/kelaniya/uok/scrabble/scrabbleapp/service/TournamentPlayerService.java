package lk.kelaniya.uok.scrabble.scrabbleapp.service;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TournamentPlayerDTO;

import java.util.List;

public interface TournamentPlayerService {

    /** Register an existing player to a tournament */
    TournamentPlayerDTO registerPlayer(String tournamentId, String playerId);

    /** Remove a player registration from a tournament */
    void removePlayer(String tournamentPlayerId);

    /** Get all registered players for a given tournament */
    List<TournamentPlayerDTO> getPlayersByTournament(String tournamentId);

    /**
     * Check all players registered in "Mini Tournament Uok".
     * Any player who has NOT played in 3 consecutive rounds is marked INACTIVE.
     * Call this after every game is added/deleted.
     */
    void checkAndUpdateInactivePlayersForMiniTournament();
}