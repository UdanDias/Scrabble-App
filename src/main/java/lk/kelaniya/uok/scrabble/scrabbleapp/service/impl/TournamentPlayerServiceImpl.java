package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.*;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TournamentPlayerDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.PlayerActivityStatus;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.*;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PlayerNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.RoundNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.TournamentNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.TournamentPlayerService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.PerformanceCalc;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.UtilData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
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
    private final InactivityWindowDao inactivityWindowDao;

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
        entity.setTournament(tournament);
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

    /**
     * Runs after every game change and round completion.
     *
     * When 3 consecutive rounds are missed:
     *   → mark INACTIVE
     *   → open a new InactivityWindow with fromRound = FIRST of those 3 rounds
     *     (not the detection round — this is where the player actually became unavailable)
     *
     * When an inactive player plays again:
     *   → mark ACTIVE
     *   → close the open window by setting returnRound = round they came back
     *
     * Multiple windows per player are supported:
     *   e.g. [(3, 9), (12, null)]  means inactive rounds 3-8, active 9-11, inactive again from 12
     */
    @Override
    public void checkAndUpdateInactivePlayersForMiniTournament() {
        List<TournamentPlayerEntity> registrations =
                tournamentPlayerDao.findByTournamentName(MINI_TOURNAMENT_NAME);

        if (registrations.isEmpty()) return;

        String tournamentId = registrations.get(0).getTournament().getTournamentId();

        List<RoundEntity> completedRounds = roundDao
                .findByTournament_TournamentIdOrderByRoundNumberAsc(tournamentId)
                .stream()
                .filter(RoundEntity::isCompleted)
                .collect(Collectors.toList());

        if (completedRounds.size() < CONSECUTIVE_MISS_THRESHOLD) {
            // Not enough completed rounds yet — keep everyone active
            for (TournamentPlayerEntity reg : registrations) {
                if (reg.getActivityStatus() == PlayerActivityStatus.INACTIVE) {
                    closeOpenWindow(reg.getPlayerId(), tournamentId, 1);
                    reg.setActivityStatus(PlayerActivityStatus.ACTIVE);
                    tournamentPlayerDao.save(reg);
                }
            }
            return;
        }

        // The last 3 completed rounds
        List<RoundEntity> lastThree = completedRounds.subList(
                completedRounds.size() - CONSECUTIVE_MISS_THRESHOLD, completedRounds.size());

        // fromRound = FIRST of the 3 missed rounds (where inactivity actually started)
        int missStreakStartRound = lastThree.get(0).getRoundNumber();

        // Build set of players who played in each of the last 3 rounds
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
            String pid         = registration.getPlayerId();
            int registeredFrom = registration.getRegisteredFromRoundNumber();

            // Only consider rounds the player was actually registered for
            List<RoundEntity> relevantRounds     = new ArrayList<>();
            List<Set<String>> relevantPlayerSets = new ArrayList<>();
            for (int i = 0; i < lastThree.size(); i++) {
                if (lastThree.get(i).getRoundNumber() >= registeredFrom) {
                    relevantRounds.add(lastThree.get(i));
                    relevantPlayerSets.add(playersPerRound.get(i));
                }
            }

            if (relevantRounds.size() < CONSECUTIVE_MISS_THRESHOLD) {
                // Registered too recently — keep active
                registration.setActivityStatus(PlayerActivityStatus.ACTIVE);
                tournamentPlayerDao.save(registration);
                continue;
            }

            boolean missedAll3 = relevantPlayerSets.stream()
                    .noneMatch(roundPlayers -> roundPlayers.contains(pid));

            if (missedAll3) {
                // ── INACTIVE ──────────────────────────────────────────────────
                // Only open a new window if there isn't already one open
                boolean windowAlreadyOpen = inactivityWindowDao
                        .findByPlayerIdAndTournamentIdAndReturnRoundIsNull(pid, tournamentId)
                        .isPresent();

                if (!windowAlreadyOpen) {
                    // Open a new inactivity window starting at the FIRST missed round
                    InactivityWindowEntity window = InactivityWindowEntity.builder()
                            .playerId(pid)
                            .tournamentId(tournamentId)
                            .fromRound(missStreakStartRound) // ← first of the 3 missed rounds
                            .returnRound(null)               // ← still inactive
                            .build();
                    inactivityWindowDao.save(window);
                }

                registration.setActivityStatus(PlayerActivityStatus.INACTIVE);

            } else {
                // ── ACTIVE ────────────────────────────────────────────────────
                if (registration.getActivityStatus() == PlayerActivityStatus.INACTIVE) {
                    // Player came back — find the earliest of the last 3 rounds they played in
                    int returnRound = findReturnRound(pid, relevantRounds, relevantPlayerSets);
                    // Close their open inactivity window
                    closeOpenWindow(pid, tournamentId, returnRound);
                }
                registration.setActivityStatus(PlayerActivityStatus.ACTIVE);
            }

            tournamentPlayerDao.save(registration);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Finds the earliest round in the last 3 where the player appeared.
     * This is the round they "came back" — their returnRound for the window.
     */
    private int findReturnRound(String playerId,
                                List<RoundEntity> rounds,
                                List<Set<String>> playersPerRound) {
        for (int i = 0; i < rounds.size(); i++) {
            if (playersPerRound.get(i).contains(playerId)) {
                return rounds.get(i).getRoundNumber();
            }
        }
        return rounds.get(rounds.size() - 1).getRoundNumber();
    }

    /**
     * Closes any currently open inactivity window (returnRound = null)
     * by setting returnRound to the round the player came back.
     */
    private void closeOpenWindow(String playerId, String tournamentId, int returnRound) {
        inactivityWindowDao
                .findByPlayerIdAndTournamentIdAndReturnRoundIsNull(playerId, tournamentId)
                .ifPresent(window -> {
                    window.setReturnRound(returnRound);
                    inactivityWindowDao.save(window);
                });
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private TournamentPlayerDTO toDTO(TournamentPlayerEntity entity) {
        TournamentPlayerDTO dto = new TournamentPlayerDTO();
        dto.setTournamentPlayerId(entity.getTournamentPlayerId());
        dto.setTournamentId(entity.getTournament().getTournamentId());
        dto.setTournamentName(entity.getTournamentName());
        dto.setPlayerId(entity.getPlayerId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setActivityStatus(entity.getActivityStatus());
        return dto;
    }
}