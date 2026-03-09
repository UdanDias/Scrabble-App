package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.GameDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.PlayerDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.RoundDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.TournamentDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.TournamentPlayerDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TournamentPlayerDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.GameEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PlayerEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.RoundEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.TournamentEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.TournamentPlayerEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.PlayerActivityStatus;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PlayerNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.TournamentNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.TournamentPlayerService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.UtilData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TournamentPlayerServiceImpl implements TournamentPlayerService {

    private static final String MINI_TOURNAMENT_NAME = "Mini Tournament Uok";
    private static final int CONSECUTIVE_MISS_THRESHOLD = 3;

    private final TournamentPlayerDao tournamentPlayerDao;
    private final TournamentDao tournamentDao;
    private final PlayerDao playerDao;
    private final RoundDao roundDao;
    private final GameDao gameDao;

    // ── Register ─────────────────────────────────────────────────────────────

    @Override
    public TournamentPlayerDTO registerPlayer(String tournamentId, String playerId) {

        // Prevent duplicate registrations
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
        entity.setTournamentPlayerId(UtilData.generateTournamentPlayerId()); // add this helper — see note below
        entity.setTournamentId(tournamentId);
        entity.setTournamentName(tournament.getTournamentName());
        entity.setPlayerId(playerId);
        entity.setFirstName(player.getFirstName());
        entity.setLastName(player.getLastName());
        entity.setActivityStatus(PlayerActivityStatus.ACTIVE);

        TournamentPlayerEntity saved = tournamentPlayerDao.save(entity);
        return toDTO(saved);
    }

    // ── Remove ───────────────────────────────────────────────────────────────

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

    // ── Inactivity check ─────────────────────────────────────────────────────

    /**
     * Logic:
     * 1. Find the tournament named "Mini Tournament Uok".
     * 2. Get all its rounds ordered by roundNumber ASC.
     * 3. For each registered player, look at the last 3 rounds.
     *    If the player did NOT appear in any game in all 3 of those rounds → INACTIVE.
     *    Otherwise → ACTIVE.
     *
     * Only runs if there are at least 3 rounds in the tournament.
     */
    @Override
    public void checkAndUpdateInactivePlayersForMiniTournament() {

        // 1. Find the tournament by name
        List<TournamentPlayerEntity> registrations =
                tournamentPlayerDao.findByTournamentName(MINI_TOURNAMENT_NAME);

        if (registrations.isEmpty()) return;

        String tournamentId = registrations.get(0).getTournamentId();

        // 2. Get all rounds for this tournament ordered by roundNumber
        List<RoundEntity> rounds = roundDao.findByTournament_TournamentIdOrderByRoundNumberAsc(tournamentId);

        // Need at least CONSECUTIVE_MISS_THRESHOLD rounds to apply the rule
        if (rounds.size() < CONSECUTIVE_MISS_THRESHOLD) return;

        // 3. Take the last 3 rounds
        List<RoundEntity> lastThreeRounds = rounds.subList(
                rounds.size() - CONSECUTIVE_MISS_THRESHOLD, rounds.size());

        // 4. For each of those rounds, collect all player IDs who played
        //    (player1 or player2 in any game of that round)
        List<Set<String>> playersPerRound = lastThreeRounds.stream()
                .map(round -> {
                    Set<String> playerIds = new HashSet<>();
                    for (GameEntity game : round.getGames()) {
                        if (game.getPlayer1() != null)
                            playerIds.add(game.getPlayer1().getPlayerId());
                        if (game.getPlayer2() != null)
                            playerIds.add(game.getPlayer2().getPlayerId());
                    }
                    return playerIds;
                })
                .collect(Collectors.toList());

        // 5. Evaluate each registered player
        for (TournamentPlayerEntity registration : registrations) {
            String pid = registration.getPlayerId();

            // Check if the player missed ALL 3 consecutive rounds
            boolean missedAll3 = playersPerRound.stream()
                    .noneMatch(roundPlayers -> roundPlayers.contains(pid));

            if (missedAll3) {
                registration.setActivityStatus(PlayerActivityStatus.INACTIVE);
            } else {
                registration.setActivityStatus(PlayerActivityStatus.ACTIVE);
            }

            tournamentPlayerDao.save(registration);
        }
    }

    // ── Mapper ───────────────────────────────────────────────────────────────

    private TournamentPlayerDTO toDTO(TournamentPlayerEntity entity) {
        TournamentPlayerDTO dto = new TournamentPlayerDTO();
        dto.setTournamentPlayerId(entity.getTournamentPlayerId());
        dto.setTournamentId(entity.getTournamentId());
        dto.setTournamentName(entity.getTournamentName());
        dto.setPlayerId(entity.getPlayerId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setActivityStatus(entity.getActivityStatus());
        return dto;
    }
}