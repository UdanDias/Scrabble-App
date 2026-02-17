package lk.kelaniya.uok.scrabble.scrabbleapp.controller;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.GameDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/game")
public class GameController {
    @GetMapping
    public String health(){
        return "health is running";
    }
    @PostMapping("/addgame")
    public ResponseEntity<Void> addGame(@RequestBody GameDTO gameDTO){
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
    @DeleteMapping("/deletegame")
    public ResponseEntity<Void> deleteGame(@RequestParam("gameId") String gameId){
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    @PatchMapping("/updategame")
    public ResponseEntity<Void> updateGame(@RequestParam("gameId")String gameId , @RequestBody GameDTO gameDTO){
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    @GetMapping("/getselectedgame")
    public ResponseEntity<GameDTO> getSelectedGame(@RequestParam("gameId") String gameId){
        return ResponseEntity.ok(new GameDTO( "G001",
                "P001",
                "P002",
                450,
                420,
                30,
                "P001",
                LocalDate.of(2025, 2, 10)));
    }
    @GetMapping("/getallgames")
    public ResponseEntity<List<GameDTO>> getAllGames(){
        List<GameDTO> gameDTOList=new ArrayList<>();
        gameDTOList.add(new GameDTO( "G001",
                "P001",
                "P002",
                450,
                420,
                30,
                "P001",
                LocalDate.of(2025, 2, 10)));
        gameDTOList.add(new GameDTO(
                "G002",
                "P003",
                "P004",
                390,
                410,
                20,
                "P004",
                LocalDate.of(2025, 2, 12))
        );
        gameDTOList.add(new GameDTO(
                "G003",
                "P002",
                "P005",
                500,
                480,
                20,
                "P002",
                LocalDate.of(2025, 2, 15))
        );
        return ResponseEntity.ok(gameDTOList);
    }
}
