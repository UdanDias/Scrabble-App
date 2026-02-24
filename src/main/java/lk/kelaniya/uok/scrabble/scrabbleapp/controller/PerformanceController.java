package lk.kelaniya.uok.scrabble.scrabbleapp.controller;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PerformanceDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PlayerDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.RankedPlayerDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PerformanceNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/performance")
@RequiredArgsConstructor
public class PerformanceController {
    private final PerformanceService performanceService;
    @GetMapping
    public String healthCheck(){
        return "Health controller is running";
    }

    @GetMapping("/getselectedperformance")
    public ResponseEntity<PerformanceDTO> getSelectedPerformance(@RequestParam ("performanceId")String performanceId){
        if(performanceId==null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }try {
            performanceService.getSelectedPerformance(performanceId);
            return ResponseEntity.ok(performanceService.getSelectedPerformance(performanceId));
        }catch (PerformanceNotFoundException e){
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/getallperformances")
    public ResponseEntity<List<PerformanceDTO>> getAllPerformances() {
        try {
            performanceService.getAllPerformances();
            return ResponseEntity.ok(performanceService.getAllPerformances());
        } catch (PerformanceNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/getrankedplayers")
    public ResponseEntity<List<RankedPlayerDTO>> getRankedPlayers() {
        try {
            return ResponseEntity.ok(performanceService.getPlayersOrderedByRank());
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
