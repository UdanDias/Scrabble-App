package lk.kelaniya.uok.scrabble.scrabbleapp.service;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TournamentTeamDTO;

import java.util.List;

public interface TournamentTeamService {

    TournamentTeamDTO registerTeam(String tournamentId, String teamId);

    void removeTeam(String tournamentTeamId);

    List<TournamentTeamDTO> getTeamsByTournament(String tournamentId);
}