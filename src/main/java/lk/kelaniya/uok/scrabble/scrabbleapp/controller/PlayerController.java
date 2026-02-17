package lk.kelaniya.uok.scrabble.scrabbleapp.controller;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PlayerDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/scrabble")
public class PlayerController {
    @GetMapping
    public String healthCheck(){
        return "Health controller is running";
    }
    @PostMapping(value = "/addplayer",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addPlayer(@RequestBody PlayerDTO playerDTO){
        System.out.println(playerDTO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
    @DeleteMapping("/deleteplayer")
    public ResponseEntity<Void> deletePlayer(@RequestParam("playerId") String playerId){
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    @PatchMapping(value = "/updateplayer",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updatePlayer(@RequestParam ("playerId") String playerId, @RequestBody PlayerDTO playerDTO){
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    @GetMapping("/getselectedplayer")
    public ResponseEntity<PlayerDTO> getPlayer(@RequestParam ("playerId")String playerId){
        return ResponseEntity.ok(new PlayerDTO("P001",
                "Udan",
                "Dias",
                22,
                "Male",
                LocalDate.of(2003, 5, 14),
                "udan.dias@example.com",
                "0771234567",
                "Kelaniya, Sri Lanka",
                "Faculty of Computing and Technology",
                "Undergraduate",
                LocalDate.of(2024, 1, 10),
                15.0f,
                320,
                21.3f,
                5));
    }
    @GetMapping("/getallplayers")
    public ResponseEntity<List<PlayerDTO>> getAllPlayers(){
        List<PlayerDTO> playerDTOList=new ArrayList<>();
        playerDTOList.add(new PlayerDTO(
                "P001",
                "Udan",
                "Dias",
                22,
                "Male",
                LocalDate.of(2003, 5, 14),
                "udan.dias@example.com",
                "0771234567",
                "Kelaniya, Sri Lanka",
                "Faculty of Computing and Technology",
                "Undergraduate",
                LocalDate.of(2024, 1, 10),
                15.0f,
                320,
                21.3f,
                5));
        playerDTOList.add(new PlayerDTO(
                "P002",
                "Nadil",
                "Perera",
                23,
                "Male",
                LocalDate.of(2002, 3, 2),
                "nadil.perera@example.com",
                "0779876543",
                "Colombo",
                "Faculty of Science",
                "Undergraduate",
                LocalDate.of(2023, 11, 5),
                22.0f,
                410,
                18.6f,
                3));
        return ResponseEntity.ok(playerDTOList);
    }
}
