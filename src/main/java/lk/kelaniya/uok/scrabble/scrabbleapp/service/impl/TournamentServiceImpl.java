package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.TournamentDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TournamentDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.TournamentEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.TournamentNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.TournamentService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.UtilData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TournamentServiceImpl implements TournamentService {

    private final TournamentDao tournamentDao;

    @Override
    public void addTournament(TournamentDTO tournamentDTO) {
        TournamentEntity entity = new TournamentEntity();
        entity.setTournamentId(UtilData.generateTournamentId());
        entity.setTournamentName(tournamentDTO.getTournamentName());
        entity.setStatus(tournamentDTO.getStatus());
        entity.setTournamentType(tournamentDTO.getTournamentType()); // ← added
        tournamentDao.save(entity);
    }

    @Override
    public TournamentDTO getSelectedTournament(String tournamentId) {
        TournamentEntity entity = tournamentDao.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException("Tournament not found: " + tournamentId));
        return mapToDTO(entity);
    }

    @Override
    public List<TournamentDTO> getAllTournaments() {
        return tournamentDao.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TournamentDTO updateTournament(String tournamentId, TournamentDTO tournamentDTO) {
        TournamentEntity entity = tournamentDao.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException("Tournament not found: " + tournamentId));
        entity.setTournamentName(tournamentDTO.getTournamentName());
        entity.setStatus(tournamentDTO.getStatus());
        return mapToDTO(tournamentDao.save(entity));
    }

    @Override
    public void deleteTournament(String tournamentId) {
        TournamentEntity entity = tournamentDao.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException("Tournament not found: " + tournamentId));
        tournamentDao.delete(entity);
    }

    private TournamentDTO mapToDTO(TournamentEntity entity) {
        TournamentDTO dto = new TournamentDTO();
        dto.setTournamentId(entity.getTournamentId());
        dto.setTournamentName(entity.getTournamentName());
        dto.setStatus(entity.getStatus());
        dto.setTournamentType(entity.getTournamentType()); // ← added
        return dto;
    }
}