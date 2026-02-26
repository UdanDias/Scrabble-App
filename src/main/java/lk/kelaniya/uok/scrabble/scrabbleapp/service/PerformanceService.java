package lk.kelaniya.uok.scrabble.scrabbleapp.service;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PerformanceDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PlayerDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.RankedPlayerDTO;

import java.util.List;

public interface PerformanceService {

    PerformanceDTO getSelectedPerformance(String playerId);
    List<PerformanceDTO> getAllPerformances();
    List<RankedPlayerDTO> getPlayersOrderedByRank();
}
