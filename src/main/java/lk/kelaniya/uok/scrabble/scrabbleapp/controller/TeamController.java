package lk.kelaniya.uok.scrabble.scrabbleapp.controller;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.RankedTeamDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TeamDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TeamPairingDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping("/createteam")
    public ResponseEntity<TeamDTO> createTeam(@RequestBody TeamDTO teamDTO) {
        try {
            return new ResponseEntity<>(teamService.createTeam(teamDTO), HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getteam")
    public ResponseEntity<TeamDTO> getTeam(@RequestParam String teamId) {
        try {
            return ResponseEntity.ok(teamService.getTeam(teamId));
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/getallteams")
    public ResponseEntity<List<TeamDTO>> getAllTeams() {
        try {
            return ResponseEntity.ok(teamService.getAllTeams());
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PatchMapping("/updateteam")
    public ResponseEntity<TeamDTO> updateTeam(@RequestParam String teamId, @RequestBody TeamDTO teamDTO) {
        try {
            return ResponseEntity.ok(teamService.updateTeam(teamId, teamDTO));
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/deleteteam")
    public ResponseEntity<Void> deleteTeam(@RequestParam String teamId) {
        try {
            teamService.deleteTeam(teamId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getteamleaderboard")
    public ResponseEntity<List<RankedTeamDTO>> getTeamLeaderboard(@RequestParam String tournamentId) {
        try {
            return ResponseEntity.ok(teamService.getTeamLeaderboard(tournamentId));
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getteamswisspairing")
    public ResponseEntity<List<TeamPairingDTO>> getTeamSwissPairings(@RequestParam String tournamentId) {
        try {
            return ResponseEntity.ok(teamService.getTeamSwissPairings(tournamentId));
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}