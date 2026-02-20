package lk.kelaniya.uok.scrabble.scrabbleapp.controller;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PlayerDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PlayerNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/player")
@RequiredArgsConstructor
public class PlayerController {
    private final PlayerService playerService;
    @GetMapping
    public String healthCheck(){
        return "Health controller is running";
    }
    @PostMapping(value = "/addplayer",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addPlayer(@RequestBody PlayerDTO playerDTO){

        if(playerDTO==null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            playerService.addPlayer(playerDTO);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (PlayerNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);        }

    }
    @DeleteMapping("/deleteplayer")
    public ResponseEntity<Void> deletePlayer(@RequestParam("playerId") String playerId){
        if(playerId==null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            playerService.deletePlayer(playerId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }catch (PlayerNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PatchMapping(value = "/updateplayer",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updatePlayer(@RequestParam ("playerId") String playerId, @RequestBody PlayerDTO playerDTO){
        if(playerId==null||playerDTO==null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            playerService.updatePlayer(playerId, playerDTO);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }catch (PlayerNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/getselectedplayer")
    public ResponseEntity<PlayerDTO> getSelectedPlayer(@RequestParam ("playerId")String playerId){
        if(playerId==null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            return ResponseEntity.ok(playerService.getSelectedPlayer(playerId));
        }catch (PlayerNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/getallplayers")
    public ResponseEntity<List<PlayerDTO>> getAllPlayers(){
        try {
            return ResponseEntity.ok(playerService.getAllPlayers());
        }catch (PlayerNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
