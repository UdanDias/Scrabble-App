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
@RequestMapping("/api/v1/player")
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
    public ResponseEntity<PlayerDTO> getSelectedPlayer(@RequestParam ("playerId")String playerId){
        return ResponseEntity.ok(new PlayerDTO("P001",                     // playerId
                "Udan",                      // firstName
                "Dias",                      // lastName
                17,                          // age
                "Male",                      // gender
                LocalDate.of(2009, 5, 10),   // dob
                "udan.dias@example.com",      // email
                "+94771234567",               // phone
                "123 Main St, Colombo",       // address
                "Computing",                 // faculty
                "Undergraduate",             // academicLevel
                LocalDate.of(2025, 9, 1)  // accountCreatedDate
                ));
    }
    @GetMapping("/getallplayers")
    public ResponseEntity<List<PlayerDTO>> getAllPlayers(){
        List<PlayerDTO> playerDTOList=new ArrayList<>();
        playerDTOList.add(new PlayerDTO(
                "P001",                     // playerId
                "Udan",                      // firstName
                "Dias",                      // lastName
                17,                          // age
                "Male",                      // gender
                LocalDate.of(2009, 5, 10),   // dob
                "udan.dias@example.com",      // email
                "+94771234567",               // phone
                "123 Main St, Colombo",       // address
                "Computing",                 // faculty
                "Undergraduate",             // academicLevel
                LocalDate.of(2025, 9, 1)));
        playerDTOList.add(new PlayerDTO(
                "P002",                     // playerId
                "Sanjana",                   // firstName
                "Perera",                    // lastName
                16,                          // age
                "Female",                    // gender
                LocalDate.of(2010, 8, 22),   // dob
                "sanjana.perera@example.com",// email
                "+94779876543",              // phone
                "45 Lake Rd, Kandy",         // address
                "Engineering",               // faculty
                "Undergraduate",             // academicLevel
                LocalDate.of(2025, 6, 15)));
        return ResponseEntity.ok(playerDTOList);
    }
}
