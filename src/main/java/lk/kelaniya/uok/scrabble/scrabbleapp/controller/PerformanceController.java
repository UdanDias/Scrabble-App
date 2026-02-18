package lk.kelaniya.uok.scrabble.scrabbleapp.controller;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PerformanceDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PlayerDTO;
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
    @PostMapping(value = "/addperformance",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addPerformance(@RequestBody PerformanceDTO performanceDTO){
        if(performanceDTO==null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            System.out.println(performanceDTO);
            performanceService.addPerformance(performanceDTO);
            return new ResponseEntity<>(HttpStatus.CREATED);
        }catch (PerformanceNotFoundException e){
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
    @DeleteMapping("/deletePerformance")
    public ResponseEntity<Void> deletePerformance(@RequestParam("performanceId") String performanceId){
        if(performanceId==null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }try {
            performanceService.deletePerformance(performanceId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }catch (PerformanceNotFoundException e){
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PatchMapping(value = "/updateperformance",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updatePerformance(@RequestParam ("performanceId") String performanceId, @RequestBody PerformanceDTO performanceDTO){
        if(performanceId==null||performanceDTO==null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            performanceService.updatePerformance(performanceId, performanceDTO);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }catch (PerformanceNotFoundException e){
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
}
