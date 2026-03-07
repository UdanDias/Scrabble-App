package lk.kelaniya.uok.scrabble.scrabbleapp.service;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.RankedTeamDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TeamDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TeamPairingDTO;

import java.util.List;

public interface TeamService {
    TeamDTO createTeam(TeamDTO teamDTO);
    TeamDTO getTeam(String teamId);
    List<TeamDTO> getAllTeams();
    TeamDTO updateTeam(String teamId, TeamDTO teamDTO);
    void deleteTeam(String teamId);

    // Team leaderboard for a tournament
    List<RankedTeamDTO> getTeamLeaderboard(String tournamentId);

    // Swiss pairings for teams in a tournament
    List<TeamPairingDTO> getTeamSwissPairings(String tournamentId);
}