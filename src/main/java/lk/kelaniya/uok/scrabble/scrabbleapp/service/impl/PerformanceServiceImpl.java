package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.PerformanceDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PerformanceDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PlayerDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.RankedPlayerDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PerformanceEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PerformanceNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.PerformanceService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.EntityDTOConvert;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.UtilData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PerformanceServiceImpl implements PerformanceService {
    private final PerformanceDao performanceDao;
    private final EntityDTOConvert entityDTOConvert;

    @Override
    public PerformanceDTO getSelectedPerformance(String performanceId) {
        PerformanceEntity performance=performanceDao.findById(performanceId)
                .orElseThrow(()->new PerformanceNotFoundException("performance not found"));
        return entityDTOConvert.convertPerformanceEntityToPerformanceDTO(performance);
    }

    @Override
    public List<PerformanceDTO> getAllPerformances() {
        return entityDTOConvert.convertPerformanceEntityListToPerformanceDTOList(performanceDao.findAll());
    }

    @Override
    public List<RankedPlayerDTO> getPlayersOrderedByRank() {
        List<PerformanceEntity> performances = performanceDao.getAllPerformancesOrderedByRank();
        return performances.stream()
                .map(entityDTOConvert::convertPerformanceEntityToRankedPlayerDTO)
                .collect(Collectors.toList());
    }
}
