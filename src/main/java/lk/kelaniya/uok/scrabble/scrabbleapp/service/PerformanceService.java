package lk.kelaniya.uok.scrabble.scrabbleapp.service;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PerformanceDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PlayerDTO;

import java.util.List;

public interface PerformanceService {
    void addPerformance(PerformanceDTO performanceDTO);
    void deletePerformance(String performanceId);
    void updatePerformance(String performanceId,PerformanceDTO performanceDTO);
    PerformanceDTO getSelectedPerformance(String performanceId);
    List<PerformanceDTO> getAllPerformances();
}
