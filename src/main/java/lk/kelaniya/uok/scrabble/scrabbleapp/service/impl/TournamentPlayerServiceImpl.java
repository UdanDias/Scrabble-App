package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.GameDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.PlayerDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.RoundDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.TournamentDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.TournamentPlayerDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TournamentPlayerDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.PlayerActivityStatus;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.GameEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PlayerEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.RoundEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.TournamentEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.TournamentPlayerEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PlayerNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.RoundNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.TournamentNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.TournamentPlayerService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.PerformanceCalc;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.UtilData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TournamentPlayerServiceImpl implements TournamentPlayerService {

    private static final String MINI_TOURNAMENT_NAME       = "Mini Tournament Uok";
    private static final int    CONSECUTIVE_MISS_THRESHOLD = 3;

    private final TournamentPlayerDao tournamentPlayerDao;
    private final TournamentDao       tournamentDao;
    private final PlayerDao           playerDao;
    private final RoundDao            roundDao;
    private final GameDao             gameDao;
    private final PerformanceCalc     performanceCalc;

    // ── Register ──────────────────────────────────────────────────────────────

    @Override
    public TournamentPlayerDTO registerPlayer(String tournamentId, String playerId) {
        tournamentPlayerDao.findByTournamentIdAndPlayerId(tournamentId, playerId)
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "Player " + playerId + " is already registered in tournament " + tournamentId);
                });

        TournamentEntity tournament = tournamentDao.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException("Tournament not found: " + tournamentId));

        PlayerEntity player = playerDao.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found: " + playerId));

        TournamentPlayerEntity entity = new TournamentPlayerEntity();
        entity.setTournamentPlayerId(UtilData.generateTournamentPlayerId());
        entity.setTournament(tournament);                          // ← was setTournamentId()
        entity.setTournamentName(tournament.getTournamentName());
        entity.setPlayerId(playerId);
        entity.setFirstName(player.getFirstName());
        entity.setLastName(player.getLastName());
        entity.setActivityStatus(PlayerActivityStatus.ACTIVE);
        entity.setUsername(player.getUsername());

        int currentRoundNumber = roundDao
                .findByTournament_TournamentIdOrderByRoundNumberAsc(tournamentId)
                .stream()
                .filter(r -> !r.isCompleted())
                .mapToInt(RoundEntity::getRoundNumber)
                .min()
                .orElse(1);

        entity.setRegisteredFromRoundNumber(currentRoundNumber);

        return toDTO(tournamentPlayerDao.save(entity));
    }

    // ── Remove ────────────────────────────────────────────────────────────────

    @Override
    public void removePlayer(String tournamentPlayerId) {
        TournamentPlayerEntity entity = tournamentPlayerDao.findById(tournamentPlayerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "TournamentPlayer record not found: " + tournamentPlayerId));
        tournamentPlayerDao.delete(entity);
    }

    // ── Get by tournament ─────────────────────────────────────────────────────

    @Override
    public List<TournamentPlayerDTO> getPlayersByTournament(String tournamentId) {
        return tournamentPlayerDao.findByTournamentId(tournamentId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── Complete round ────────────────────────────────────────────────────────

    @Override
    public void completeRound(String roundId) {
        RoundEntity round = roundDao.findById(roundId)
                .orElseThrow(() -> new RoundNotFoundException("Round not found: " + roundId));

        if (round.isCompleted()) return;

        round.setCompleted(true);
        roundDao.save(round);

        performanceCalc.reCalculateAllPerformances();
        checkAndUpdateInactivePlayersForMiniTournament();
    }

    // ── Inactivity check ──────────────────────────────────────────────────────

    @Override
    public void checkAndUpdateInactivePlayersForMiniTournament() {
        List<TournamentPlayerEntity> registrations =
                tournamentPlayerDao.findByTournamentName(MINI_TOURNAMENT_NAME);

        if (registrations.isEmpty()) return;

        // ← was getTournamentId(), now goes through the relationship
        String tournamentId = registrations.get(0).getTournament().getTournamentId();

        List<RoundEntity> completedRounds = roundDao
                .findByTournament_TournamentIdOrderByRoundNumberAsc(tournamentId)
                .stream()
                .filter(RoundEntity::isCompleted)
                .collect(Collectors.toList());

        if (completedRounds.size() < CONSECUTIVE_MISS_THRESHOLD) {
            for (TournamentPlayerEntity reg : registrations) {
                reg.setActivityStatus(PlayerActivityStatus.ACTIVE);
                tournamentPlayerDao.save(reg);
            }
            return;
        }

        List<RoundEntity> lastThree = completedRounds.subList(
                completedRounds.size() - CONSECUTIVE_MISS_THRESHOLD, completedRounds.size());

        List<Set<String>> playersPerRound = lastThree.stream()
                .map(r -> {
                    Set<String> ids = new HashSet<>();
                    for (GameEntity game : gameDao.findByRound_RoundId(r.getRoundId())) {
                        if (game.getPlayer1() != null) ids.add(game.getPlayer1().getPlayerId());
                        if (game.getPlayer2() != null) ids.add(game.getPlayer2().getPlayerId());
                    }
                    return ids;
                })
                .collect(Collectors.toList());

        for (TournamentPlayerEntity registration : registrations) {
            String pid = registration.getPlayerId();
            int registeredFrom = registration.getRegisteredFromRoundNumber();

            List<Set<String>> relevantRounds = new ArrayList<>();
            for (int i = 0; i < lastThree.size(); i++) {
                if (lastThree.get(i).getRoundNumber() >= registeredFrom) {
                    relevantRounds.add(playersPerRound.get(i));
                }
            }

            if (relevantRounds.size() < CONSECUTIVE_MISS_THRESHOLD) {
                registration.setActivityStatus(PlayerActivityStatus.ACTIVE);
                tournamentPlayerDao.save(registration);
                continue;
            }

            boolean missedAll3 = relevantRounds.stream()
                    .noneMatch(roundPlayers -> roundPlayers.contains(pid));

            registration.setActivityStatus(
                    missedAll3 ? PlayerActivityStatus.INACTIVE : PlayerActivityStatus.ACTIVE
            );
            tournamentPlayerDao.save(registration);
        }
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private TournamentPlayerDTO toDTO(TournamentPlayerEntity entity) {
        TournamentPlayerDTO dto = new TournamentPlayerDTO();
        dto.setTournamentPlayerId(entity.getTournamentPlayerId());
        dto.setTournamentId(entity.getTournament().getTournamentId());  // ← was getTournamentId()
        dto.setTournamentName(entity.getTournamentName());
        dto.setPlayerId(entity.getPlayerId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setActivityStatus(entity.getActivityStatus());
        return dto;
    }
}