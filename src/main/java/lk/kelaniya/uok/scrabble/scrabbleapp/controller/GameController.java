package lk.kelaniya.uok.scrabble.scrabbleapp.controller;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.ByeGameDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.GameDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.GameNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PlayerNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/game")
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;
    @GetMapping
    public String health(){
        return "health is running";
    }
    @PostMapping("/addgame")
    public ResponseEntity<Void> addGame(@RequestBody GameDTO gameDTO){
        if(gameDTO==null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            gameService.addGame(gameDTO);
            return new ResponseEntity<>(HttpStatus.CREATED);
        }catch (GameNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    // ✅ Fixed controller method
    @PostMapping("/addbyegame")
    public ResponseEntity<Void> addByeGame(@RequestBody ByeGameDTO byeGameDTO) {
        if (byeGameDTO.getPlayerId() == null || byeGameDTO.getGameDate() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            gameService.addByeGame(byeGameDTO.getPlayerId(), byeGameDTO.getGameDate());
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (PlayerNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/deletegame")
    public ResponseEntity<Void> deleteGame(@RequestParam("gameId") String gameId){
        if(gameId==null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            gameService.deleteGame(gameId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }catch (GameNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PatchMapping("/updategame")
    public ResponseEntity<Void> updateGame(@RequestParam("gameId")String gameId , @RequestBody GameDTO gameDTO){
        if(gameId==null||gameDTO==null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            gameService.updateGame(gameId,gameDTO);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }catch (GameNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/getselectedgame")
    public ResponseEntity<GameDTO> getSelectedGame(@RequestParam("gameId") String gameId){
        if(gameId==null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            return ResponseEntity.ok(gameService.getSelectedGame(gameId));
        }catch (GameNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/getallgames")
    public ResponseEntity<List<GameDTO>> getAllGames(){
       try {
           return ResponseEntity.ok(gameService.getAllGames());
       }catch (GameNotFoundException e) {
           e.printStackTrace();
           return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
       }catch (Exception e) {
           e.printStackTrace();
           return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
       }
    }
}
