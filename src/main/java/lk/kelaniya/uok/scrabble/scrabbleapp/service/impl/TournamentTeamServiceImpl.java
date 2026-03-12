package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.TeamDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.TournamentDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.TournamentTeamDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TournamentTeamDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.PlayerActivityStatus;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.TeamEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.TournamentEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.TournamentTeamEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.TournamentNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.TournamentTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TournamentTeamServiceImpl implements TournamentTeamService {

    private final TournamentTeamDao tournamentTeamDao;
    private final TournamentDao     tournamentDao;
    private final TeamDao           teamDao;

    // ── Register a team into a tournament ────────────────────────────────────

    @Override
    public TournamentTeamDTO registerTeam(String tournamentId, String teamId) {
        TournamentEntity tournament = tournamentDao.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException("Tournament not found: " + tournamentId));

        TeamEntity team = teamDao.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        // Duplicate check
        tournamentTeamDao.findByTournamentIdAndTeamId(tournamentId, teamId)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Team is already registered in this tournament.");
                });

        TournamentTeamEntity entity = TournamentTeamEntity.builder()
                .tournament(tournament)
                .team(team)
                .activityStatus(PlayerActivityStatus.ACTIVE)
                .build();

        TournamentTeamEntity saved = tournamentTeamDao.save(entity);
        return toDTO(saved);
    }

    // ── Remove a team registration ────────────────────────────────────────────

    @Override
    public void removeTeam(String tournamentTeamId) {
        TournamentTeamEntity entity = tournamentTeamDao.findById(tournamentTeamId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tournament team registration not found: " + tournamentTeamId));
        tournamentTeamDao.delete(entity);
    }

    // ── Get all teams registered to a tournament ──────────────────────────────

    @Override
    public List<TournamentTeamDTO> getTeamsByTournament(String tournamentId) {
        return tournamentTeamDao.findByTournamentId(tournamentId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── Entity → DTO ──────────────────────────────────────────────────────────

    private TournamentTeamDTO toDTO(TournamentTeamEntity entity) {
        return TournamentTeamDTO.builder()
                .tournamentTeamId(entity.getTournamentTeamId())
                .tournamentId(entity.getTournament().getTournamentId())
                .teamId(entity.getTeam().getTeamId())
                .teamName(entity.getTeam().getTeamName())
                .teamSize(entity.getTeam().getTeamSize())
                .activityStatus(entity.getActivityStatus().name())
                .build();
    }
}