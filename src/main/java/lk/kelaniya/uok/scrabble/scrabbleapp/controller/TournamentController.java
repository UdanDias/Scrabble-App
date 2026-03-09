package lk.kelaniya.uok.scrabble.scrabbleapp.controller;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TournamentDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TournamentPlayerDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.TournamentNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.TournamentPlayerService;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tournament")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;
    private final TournamentPlayerService tournamentPlayerService;   // ← inject new service

    // ═══════════════════════════════════════════════════════════════
    //  Existing tournament endpoints (unchanged)
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/addtournament")
    public ResponseEntity<Void> addTournament(@RequestBody TournamentDTO tournamentDTO) {
        if (tournamentDTO == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            tournamentService.addTournament(tournamentDTO);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getselectedtournament")
    public ResponseEntity<TournamentDTO> getSelectedTournament(@RequestParam("tournamentId") String tournamentId) {
        if (tournamentId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return ResponseEntity.ok(tournamentService.getSelectedTournament(tournamentId));
        } catch (TournamentNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getalltournaments")
    public ResponseEntity<List<TournamentDTO>> getAllTournaments() {
        try {
            return ResponseEntity.ok(tournamentService.getAllTournaments());
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PatchMapping("/updatetournament")
    public ResponseEntity<TournamentDTO> updateTournament(@RequestParam("tournamentId") String tournamentId,
                                                          @RequestBody TournamentDTO tournamentDTO) {
        if (tournamentId == null || tournamentDTO == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return ResponseEntity.ok(tournamentService.updateTournament(tournamentId, tournamentDTO));
        } catch (TournamentNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/deletetournament")
    public ResponseEntity<Void> deleteTournament(@RequestParam("tournamentId") String tournamentId) {
        if (tournamentId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            tournamentService.deleteTournament(tournamentId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (TournamentNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  NEW — Tournament player registration endpoints
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/v1/tournament/registerplayer?tournamentId=xxx&playerId=yyy
     * Registers an existing player into a tournament.
     */
    @PostMapping("/registerplayer")
    public ResponseEntity<TournamentPlayerDTO> registerPlayer(
            @RequestParam("tournamentId") String tournamentId,
            @RequestParam("playerId") String playerId) {
        try {
            TournamentPlayerDTO result = tournamentPlayerService.registerPlayer(tournamentId, playerId);
            return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            // duplicate registration
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } catch (TournamentNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DELETE /api/v1/tournament/removeplayer?tournamentPlayerId=xxx
     * Removes a player registration record.
     */
    @DeleteMapping("/removeplayer")
    public ResponseEntity<Void> removePlayer(@RequestParam("tournamentPlayerId") String tournamentPlayerId) {
        try {
            tournamentPlayerService.removePlayer(tournamentPlayerId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/v1/tournament/getplayers?tournamentId=xxx
     * Returns all registered players for a tournament (used to populate the modal).
     */
    @GetMapping("/getplayers")
    public ResponseEntity<List<TournamentPlayerDTO>> getPlayersByTournament(
            @RequestParam("tournamentId") String tournamentId) {
        try {
            List<TournamentPlayerDTO> players = tournamentPlayerService.getPlayersByTournament(tournamentId);
            return ResponseEntity.ok(players);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}