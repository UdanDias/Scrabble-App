//package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;
//
//import jakarta.transaction.Transactional;
//import lk.kelaniya.uok.scrabble.scrabbleapp.dao.RoundDao;
//import lk.kelaniya.uok.scrabble.scrabbleapp.dao.TournamentDao;
//import lk.kelaniya.uok.scrabble.scrabbleapp.dto.RoundDTO;
//import lk.kelaniya.uok.scrabble.scrabbleapp.entity.RoundEntity;
//import lk.kelaniya.uok.scrabble.scrabbleapp.entity.TournamentEntity;
//import lk.kelaniya.uok.scrabble.scrabbleapp.exception.RoundNotFoundException;
//import lk.kelaniya.uok.scrabble.scrabbleapp.exception.TournamentNotFoundException;
//import lk.kelaniya.uok.scrabble.scrabbleapp.service.RoundService;
//import lk.kelaniya.uok.scrabble.scrabbleapp.util.UtilData;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@Transactional
//@RequiredArgsConstructor
//public class RoundServiceImpl implements RoundService {
//
//    private final RoundDao roundDao;
//    private final TournamentDao tournamentDao;
//
//    @Override
//    public void addRound(RoundDTO roundDTO) {
//        TournamentEntity tournament = tournamentDao.findById(roundDTO.getTournamentId())
//                .orElseThrow(() -> new TournamentNotFoundException("Tournament not found: " + roundDTO.getTournamentId()));
//        RoundEntity entity = new RoundEntity();
//        entity.setRoundId(UtilData.generateRoundId());
//        entity.setRoundNumber(roundDTO.getRoundNumber());
//        entity.setTournament(tournament);
//        roundDao.save(entity);
//    }
//
//    @Override
//    public RoundDTO getSelectedRound(String roundId) {
//        RoundEntity entity = roundDao.findById(roundId)
//                .orElseThrow(() -> new RoundNotFoundException("Round not found: " + roundId));
//        return mapToDTO(entity);
//    }
//
//    @Override
//    public List<RoundDTO> getRoundsByTournament(String tournamentId) {
//        return roundDao.findByTournament_TournamentId(tournamentId).stream()
//                .map(this::mapToDTO)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<RoundDTO> getAllRounds() {
//        return roundDao.findAll().stream()
//                .map(this::mapToDTO)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public RoundDTO updateRound(String roundId, RoundDTO roundDTO) {
//        RoundEntity entity = roundDao.findById(roundId)
//                .orElseThrow(() -> new RoundNotFoundException("Round not found: " + roundId));
//        entity.setRoundNumber(roundDTO.getRoundNumber());
//        return mapToDTO(roundDao.save(entity));
//    }
//
//    @Override
//    public void deleteRound(String roundId) {
//        RoundEntity entity = roundDao.findById(roundId)
//                .orElseThrow(() -> new RoundNotFoundException("Round not found: " + roundId));
//        roundDao.delete(entity);
//    }
//
//    private RoundDTO mapToDTO(RoundEntity entity) {
//        RoundDTO dto = new RoundDTO();
//        dto.setRoundId(entity.getRoundId());
//        dto.setRoundNumber(entity.getRoundNumber());
//        dto.setTournamentId(entity.getTournament().getTournamentId());
//        return dto;
//    }
//}

package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.RoundDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.TournamentDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.RoundDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.RoundEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.TournamentEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.RoundNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.TournamentNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.RoundService;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.TournamentPlayerService;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.TournamentService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.PerformanceCalc;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.UtilData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class RoundServiceImpl implements RoundService {

    private final RoundDao        roundDao;
    private final TournamentDao   tournamentDao;
    private final PerformanceCalc performanceCalc; // ✅ inject so we can recalc on completion
    private final TournamentPlayerService tournamentPlayerService;

    @Override
    public void addRound(RoundDTO roundDTO) {
        TournamentEntity tournament = tournamentDao.findById(roundDTO.getTournamentId())
                .orElseThrow(() -> new TournamentNotFoundException(
                        "Tournament not found: " + roundDTO.getTournamentId()));
        RoundEntity entity = new RoundEntity();
        entity.setRoundId(UtilData.generateRoundId());
        entity.setRoundNumber(roundDTO.getRoundNumber());
        entity.setCompleted(false); // new rounds always start incomplete
        entity.setTournament(tournament);
        roundDao.save(entity);
    }

    @Override
    public RoundDTO getSelectedRound(String roundId) {
        RoundEntity entity = roundDao.findById(roundId)
                .orElseThrow(() -> new RoundNotFoundException("Round not found: " + roundId));
        return mapToDTO(entity);
    }

    @Override
    public List<RoundDTO> getRoundsByTournament(String tournamentId) {
        return roundDao.findByTournament_TournamentId(tournamentId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoundDTO> getAllRounds() {
        return roundDao.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Updates a round.
     * ✅ If the round is being marked completed (completed=true), triggers a full
     *    performance recalculation so Elo + absence penalties are applied.
     */
    @Override
    public RoundDTO updateRound(String roundId, RoundDTO roundDTO) {
        RoundEntity entity = roundDao.findById(roundId)
                .orElseThrow(() -> new RoundNotFoundException("Round not found: " + roundId));

        boolean wasCompleted = entity.isCompleted();
        entity.setRoundNumber(roundDTO.getRoundNumber());
        entity.setCompleted(roundDTO.isCompleted()); // ✅ persist completed flag
        roundDao.save(entity);

        // ✅ Trigger recalculation only when a round transitions to completed
        if (!wasCompleted && roundDTO.isCompleted()) {
            performanceCalc.reCalculateAllPerformances();
        }

        return mapToDTO(entity);
    }

    @Override
    public void deleteRound(String roundId) {
        RoundEntity entity = roundDao.findById(roundId)
                .orElseThrow(() -> new RoundNotFoundException("Round not found: " + roundId));
        roundDao.delete(entity);
        performanceCalc.reCalculateAllPerformances();
        tournamentPlayerService.checkAndUpdateInactivePlayersForMiniTournament();
    }

    private RoundDTO mapToDTO(RoundEntity entity) {
        RoundDTO dto = new RoundDTO();
        dto.setRoundId(entity.getRoundId());
        dto.setRoundNumber(entity.getRoundNumber());
        dto.setCompleted(entity.isCompleted()); // ✅ expose in DTO
        dto.setTournamentId(entity.getTournament().getTournamentId());
        return dto;
    }
}