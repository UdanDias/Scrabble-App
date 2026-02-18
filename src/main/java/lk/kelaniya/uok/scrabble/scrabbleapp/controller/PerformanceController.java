package lk.kelaniya.uok.scrabble.scrabbleapp.controller;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PerformanceDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PlayerDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/performance")
public class PerformanceController {
    @GetMapping
    public String healthCheck(){
        return "Health controller is running";
    }
    @PostMapping(value = "/addperformance",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addPerformance(@RequestBody PerformanceDTO performanceDTO){
        System.out.println(performanceDTO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
    @DeleteMapping("/deletePerformance")
    public ResponseEntity<Void> deletePerformance(@RequestParam("performanceId") String performanceId){
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    @PatchMapping(value = "/updateperformance",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updatePerformance(@RequestParam ("performanceId") String performanceId, @RequestBody PerformanceDTO playerDTO){
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    @GetMapping("/getselectedperformance")
    public ResponseEntity<PerformanceDTO> getSelectedPerformance(@RequestParam ("performanceId")String performanceId){
        return ResponseEntity.ok(new PerformanceDTO(
                "PERF001",      // performanceId
                "P001",         // playerId
                18.0f,          // totalWins
                25,             // totalGamesPlayed
                320,            // cumMargin
                12.8f,          // avgMargin
                3    // accountCreatedDate
        ));
    }
    @GetMapping("/getallperformances")
    public ResponseEntity<List<PerformanceDTO>> getAllPerformances(){
        List<PerformanceDTO> performanceDTOList=new ArrayList<>();
        performanceDTOList.add(new PerformanceDTO(
                "PERF001",      // performanceId
                "P001",         // playerId
                18.0f,          // totalWins
                25,             // totalGamesPlayed
                320,            // cumMargin
                12.8f,          // avgMargin
                3  ));
        performanceDTOList.add(new PerformanceDTO(
                "PERF002",      // performanceId
                "P002",         // playerId
                12.0f,          // totalWins
                20,             // totalGamesPlayed
                150,            // cumMargin
                7.5f,           // avgMargin
                6    ));
        return ResponseEntity.ok(performanceDTOList);
    }
}
