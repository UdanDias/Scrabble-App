package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.*;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PairingDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PerformanceDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.RankedPlayerDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.PlayerActivityStatus;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.*;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PerformanceNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.PerformanceService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.EloCalculator;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.EntityDTOConvert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class PerformanceServiceImpl implements PerformanceService {

    private static final String MINI_TOURNAMENT_NAME = "Mini Tournament Uok";

    /**
     * Width of each Elo band.
     * 1400-1499 → band 14, 1300-1399 → band 13, etc.
     */
    private static final int ELO_BAND_WIDTH = 100;

    private final PerformanceDao      performanceDao;
    private final EntityDTOConvert    entityDTOConvert;
    private final GameDao             gameDao;
    private final RoundDao            roundDao;
    private final TournamentPlayerDao tournamentPlayerDao;
    private final InactivityWindowDao inactivityWindowDao;

    // ── Basic queries ─────────────────────────────────────────────────────────

    @Override
    public PerformanceDTO getSelectedPerformance(String playerId) {
        PerformanceEntity performance = performanceDao.findById(playerId)
                .orElseThrow(() -> new PerformanceNotFoundException("performance not found"));
        return entityDTOConvert.convertPerformanceEntityToPerformanceDTO(performance);
    }

    @Override
    public List<PerformanceDTO> getAllPerformances() {
        return entityDTOConvert.convertPerformanceEntityListToPerformanceDTOList(performanceDao.findAll());
    }

    @Override
    public List<RankedPlayerDTO> getPlayersOrderedByRank() {
        return List.of();
    }

    // ── Tournament leaderboard ────────────────────────────────────────────────

    public List<RankedPlayerDTO> getPlayersOrderedByRankByTournament(String tournamentId) {
        List<GameEntity> tournamentGames = gameDao.getGamesByTournamentId(tournamentId);
        if (tournamentGames.isEmpty()) return List.of();

        boolean isMiniTournament = tournamentGames.stream()
                .filter(g -> g.getRound() != null && g.getRound().getTournament() != null)
                .anyMatch(g -> MINI_TOURNAMENT_NAME.equals(
                        g.getRound().getTournament().getTournamentName()));

        if (!isMiniTournament) {
            return replayNonMiniTournament(tournamentGames);
        }

        List<TournamentPlayerEntity> registrations =
                tournamentPlayerDao.findByTournamentId(tournamentId);

        Set<String> activePlayerIds = registrations.stream()
                .filter(tp -> tp.getActivityStatus() != PlayerActivityStatus.INACTIVE)
                .map(TournamentPlayerEntity::getPlayerId)
                .collect(Collectors.toSet());

        Map<String, List<InactivityWindowEntity>> freezeWindows = new HashMap<>();
        inactivityWindowDao.findByTournamentId(tournamentId).forEach(w ->
                freezeWindows.computeIfAbsent(w.getPlayerId(), k -> new ArrayList<>()).add(w));

        List<RoundEntity> allRounds = roundDao.findByTournament_TournamentId(tournamentId)
                .stream()
                .sorted(Comparator.comparingInt(RoundEntity::getRoundNumber))
                .collect(Collectors.toList());

        Map<String, List<GameEntity>> gamesByRound = new HashMap<>();
        for (GameEntity game : tournamentGames) {
            if (game.getRound() == null) continue;
            gamesByRound.computeIfAbsent(game.getRound().getRoundId(), k -> new ArrayList<>()).add(game);
        }

        Map<String, TournamentPlayerStats> statsMap            = new HashMap<>();
        Map<String, Double>                previousEloSnapshot = new HashMap<>();
        Map<String, Double>                lastCompletedElo    = new HashMap<>();

        for (RoundEntity round : allRounds) {
            int roundNumber = round.getRoundNumber();

            List<GameEntity> roundGames = gamesByRound.getOrDefault(round.getRoundId(), List.of())
                    .stream()
                    .sorted(Comparator.comparing(g -> g.getGameDate() != null
                            ? g.getGameDate() : java.time.LocalDate.MIN))
                    .collect(Collectors.toList());

            for (GameEntity game : roundGames) {
                processGame(game, statsMap, true, roundNumber, freezeWindows);
            }

            if (round.isCompleted()) {
                Set<String> playersWhoPlayed = gamesByRound
                        .getOrDefault(round.getRoundId(), List.of())
                        .stream()
                        .flatMap(g -> Stream.of(
                                g.getPlayer1() != null ? g.getPlayer1().getPlayerId() : null,
                                g.getPlayer2() != null ? g.getPlayer2().getPlayerId() : null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                for (TournamentPlayerEntity tp : registrations) {
                    String pid = tp.getPlayerId();
                    if (isFrozen(pid, roundNumber, freezeWindows)) continue;
                    if (tp.getRegisteredFromRoundNumber() <= roundNumber
                            && !playersWhoPlayed.contains(pid)) {
                        TournamentPlayerStats stats = statsMap.get(pid);
                        if (stats != null) stats.eloRating -= EloCalculator.ABSENCE_PENALTY;
                    }
                }

                previousEloSnapshot = new HashMap<>(lastCompletedElo);
                for (Map.Entry<String, TournamentPlayerStats> entry : statsMap.entrySet()) {
                    lastCompletedElo.put(entry.getKey(), entry.getValue().eloRating);
                }
            }
        }

        List<TournamentPlayerStats> statsList = new ArrayList<>(statsMap.values());
        statsList.forEach(s -> s.avgMargin = s.gamesPlayed > 0
                ? Math.round((double) s.cumMargin / s.gamesPlayed * 100.0) / 100.0 : 0.0);
        statsList.sort((a, b) -> Double.compare(b.eloRating, a.eloRating));

// ✅ Filter to active players BEFORE assigning ranks,
//    so ranks are contiguous (1, 2, 3...) with no gaps from inactive players.
        List<TournamentPlayerStats> activeStatsList = statsList.stream()
                .filter(s -> activePlayerIds.contains(s.playerId))
                .collect(Collectors.toList());

        int rank = 1;
        for (int i = 0; i < activeStatsList.size(); i++) {
            if (i > 0 && Double.compare(activeStatsList.get(i).eloRating,
                    activeStatsList.get(i - 1).eloRating) != 0) rank = i + 1;
            activeStatsList.get(i).rank = rank;
        }

        final Map<String, Double> prevEloFinal = previousEloSnapshot;
        final Map<String, Double> lastEloFinal  = lastCompletedElo;

        return activeStatsList.stream()   // ← was statsList.stream().filter(...)
                .map(s -> {
                    RankedPlayerDTO dto = new RankedPlayerDTO();
                    dto.setPlayerId(s.playerId);
                    dto.setFirstName(s.firstName);
                    dto.setLastName(s.lastName);
                    dto.setUsername(s.username);
                    dto.setPlayerRank(s.rank);
                    dto.setTotalWins(s.wins);
                    dto.setTotalGamesPlayed(s.gamesPlayed);
                    dto.setCumMargin(s.cumMargin);
                    dto.setAvgMargin(s.avgMargin);
                    dto.setEloRating(lastEloFinal.getOrDefault(s.playerId, EloCalculator.DEFAULT_RATING));
                    dto.setPreviousEloRating(prevEloFinal.getOrDefault(s.playerId, EloCalculator.DEFAULT_RATING));
                    dto.setProvisional(EloCalculator.isProvisional(s.gamesPlayed));
                    return dto;
                }).collect(Collectors.toList());
    }

    // ── Non-mini replay ───────────────────────────────────────────────────────

    private List<RankedPlayerDTO> replayNonMiniTournament(List<GameEntity> games) {
        Map<String, TournamentPlayerStats> statsMap = new HashMap<>();
        games.stream()
                .sorted(Comparator.comparing(g -> g.getGameDate() != null
                        ? g.getGameDate() : java.time.LocalDate.MIN))
                .forEach(game -> processGame(game, statsMap, false, 0, Collections.emptyMap()));

        List<TournamentPlayerStats> statsList = new ArrayList<>(statsMap.values());
        statsList.forEach(s -> s.avgMargin = s.gamesPlayed > 0
                ? Math.round((double) s.cumMargin / s.gamesPlayed * 100.0) / 100.0 : 0.0);
        statsList.sort((a, b) -> {
            int cmp = Double.compare(b.wins, a.wins);
            return cmp != 0 ? cmp : Double.compare(b.avgMargin, a.avgMargin);
        });

        int rank = 1;
        for (int i = 0; i < statsList.size(); i++) {
            if (i > 0) {
                if (Double.compare(statsList.get(i).wins, statsList.get(i - 1).wins) != 0
                        || Double.compare(statsList.get(i).avgMargin, statsList.get(i - 1).avgMargin) != 0)
                    rank = i + 1;
            }
            statsList.get(i).rank = rank;
        }

        return statsList.stream().map(s -> {
            RankedPlayerDTO dto = new RankedPlayerDTO();
            dto.setPlayerId(s.playerId);
            dto.setFirstName(s.firstName);
            dto.setLastName(s.lastName);
            dto.setUsername(s.username);
            dto.setPlayerRank(s.rank);
            dto.setTotalWins(s.wins);
            dto.setTotalGamesPlayed(s.gamesPlayed);
            dto.setCumMargin(s.cumMargin);
            dto.setAvgMargin(s.avgMargin);
            dto.setEloRating(null);
            dto.setPreviousEloRating(null);
            dto.setProvisional(null);
            return dto;
        }).collect(Collectors.toList());
    }

    // ── Game processor ────────────────────────────────────────────────────────

    private void processGame(GameEntity game,
                             Map<String, TournamentPlayerStats> statsMap,
                             boolean isMiniTournament,
                             int roundNumber,
                             Map<String, List<InactivityWindowEntity>> freezeWindows) {
        if (game.isBye()) {
            if (game.getPlayer1() == null) return;
            String pid = game.getPlayer1().getPlayerId();
            boolean frozen = isFrozen(pid, roundNumber, freezeWindows);
            TournamentPlayerStats stats = statsMap.computeIfAbsent(pid,
                    id -> new TournamentPlayerStats(id,
                            game.getPlayer1().getFirstName(),
                            game.getPlayer1().getLastName(),
                            game.getPlayer1().getUsername()));
            stats.gamesPlayed++;
            stats.wins++;
            stats.cumMargin += 50;
            if (isMiniTournament && !frozen)
                stats.eloRating = EloCalculator.calculateBye(stats.eloRating, stats.gamesPlayed - 1);
        } else {
            if (game.getPlayer1() == null || game.getPlayer2() == null) return;
            String p1id = game.getPlayer1().getPlayerId();
            String p2id = game.getPlayer2().getPlayerId();
            boolean p1Frozen = isFrozen(p1id, roundNumber, freezeWindows);
            boolean p2Frozen = isFrozen(p2id, roundNumber, freezeWindows);

            TournamentPlayerStats p1 = statsMap.computeIfAbsent(p1id,
                    id -> new TournamentPlayerStats(id,
                            game.getPlayer1().getFirstName(),
                            game.getPlayer1().getLastName(),
                            game.getPlayer1().getUsername()));
            TournamentPlayerStats p2 = statsMap.computeIfAbsent(p2id,
                    id -> new TournamentPlayerStats(id,
                            game.getPlayer2().getFirstName(),
                            game.getPlayer2().getLastName(),
                            game.getPlayer2().getUsername()));

            p1.gamesPlayed++;
            p2.gamesPlayed++;
            boolean tied   = game.isGameTied();
            boolean p1wins = !tied && game.getWinner() != null
                    && game.getWinner().getPlayerId().equals(p1id);
            if (!tied) { if (p1wins) p1.wins++; else p2.wins++; }
            else       { p1.wins += 0.5; p2.wins += 0.5; }
            p1.cumMargin += (game.getScore1() - game.getScore2());
            p2.cumMargin += (game.getScore2() - game.getScore1());

            if (isMiniTournament && !p1Frozen && !p2Frozen) {
                double scoreA = tied ? 0.5 : (p1wins ? 1.0 : 0.0);
                int scoreDiff = Math.abs(game.getScore1() - game.getScore2());
                double[] newRatings = EloCalculator.calculate(
                        p1.eloRating, p2.eloRating, scoreA, scoreDiff,
                        p1.gamesPlayed - 1, p2.gamesPlayed - 1);
                p1.eloRating = newRatings[0];
                p2.eloRating = newRatings[1];
            }
        }
    }

    // ── Freeze window check ───────────────────────────────────────────────────

    private boolean isFrozen(String playerId, int roundNumber,
                             Map<String, List<InactivityWindowEntity>> freezeWindows) {
        List<InactivityWindowEntity> windows = freezeWindows.get(playerId);
        if (windows == null || windows.isEmpty()) return false;
        for (InactivityWindowEntity w : windows) {
            if (roundNumber >= w.getFromRound()
                    && (w.getReturnRound() == null || roundNumber < w.getReturnRound()))
                return true;
        }
        return false;
    }

    // ── Swiss pairings entry point ────────────────────────────────────────────

    @Override
    public List<PairingDTO> getSwissPairings(String tournamentId) {
        List<RankedPlayerDTO> rankedPlayers = getPlayersOrderedByRankByTournament(tournamentId);
        if (rankedPlayers.isEmpty()) return List.of();

        boolean isMiniTournament = rankedPlayers.stream()
                .anyMatch(p -> p.getEloRating() != null);

        return isMiniTournament
                ? buildEloBandWinSubgroupPairings(rankedPlayers)
                : buildWinsPairings(rankedPlayers);
    }

    // ── Elo-band + win-subgroup Swiss pairing ─────────────────────────────────

    /**
     * Full algorithm:
     *
     * 1. Sort all players by Elo DESC.
     * 2. Bucket into Elo bands (e.g. 1300-1399 → band 13).
     * 3. Within each band, create win sub-groups (sorted by wins DESC, Elo DESC within).
     * 4. Cascade within the band (bottom-up through win sub-groups):
     *    - If a win sub-group is ODD, pull the HIGHEST Elo player from the
     *      sub-group directly below it and add them to the top of the odd group.
     *    - Repeat until all sub-groups in the band are even.
     *    - If the LOWEST win sub-group in the band is still odd after exhausting
     *      all lower sub-groups, cross-band cascade: pull the highest Elo player
     *      from the highest win sub-group of the NEXT LOWER Elo band.
     *    - If no lower band exists and the very last sub-group across all bands
     *      is still odd → that player gets a BYE.
     * 5. Pair within each (now-even) sub-group: top-half vs bottom-half by Elo.
     *    e.g. [A,B,C,D] → A vs C, B vs D
     */
    private List<PairingDTO> buildEloBandWinSubgroupPairings(List<RankedPlayerDTO> players) {

        // ── Step 1: sort by Elo DESC ──────────────────────────────────────────
        List<RankedPlayerDTO> sorted = players.stream()
                .sorted((a, b) -> Double.compare(elo(b), elo(a)))
                .collect(Collectors.toList());

        // ── Step 2: bucket into Elo bands (highest band first) ────────────────
        // TreeMap reverse order → highest band key first when iterating
        TreeMap<Integer, List<RankedPlayerDTO>> bandMap = new TreeMap<>(Collections.reverseOrder());
        for (RankedPlayerDTO p : sorted) {
            int bandKey = (int) (elo(p) / ELO_BAND_WIDTH);
            bandMap.computeIfAbsent(bandKey, k -> new ArrayList<>()).add(p);
        }

        // ── Step 3 & 4: within each band create win sub-groups and cascade ────
        // We work with an ordered list of bands (highest first) so we can
        // perform cross-band cascades into the next band.
        List<Integer> bandKeys = new ArrayList<>(bandMap.keySet()); // highest → lowest

        // For each band, build win sub-groups as a TreeMap<wins, list>
        // (reverse order = highest wins first)
        List<TreeMap<Integer, List<RankedPlayerDTO>>> allBandSubgroups = new ArrayList<>();
        for (int bandKey : bandKeys) {
            List<RankedPlayerDTO> bandPlayers = bandMap.get(bandKey);
            TreeMap<Integer, List<RankedPlayerDTO>> winGroups = new TreeMap<>(Collections.reverseOrder());
            for (RankedPlayerDTO p : bandPlayers) {
                int winKey = p.getTotalWins() != null ? p.getTotalWins().intValue() : 0;
                winGroups.computeIfAbsent(winKey, k -> new ArrayList<>()).add(p);
            }
            // each win group is already Elo-sorted DESC (inherited from step 1 sort)
            allBandSubgroups.add(winGroups);
        }

        // ── Cascade pass ──────────────────────────────────────────────────────
        // For each band, iterate win sub-groups from highest to lowest wins.
        // If a sub-group is odd, pull the top-Elo player from the sub-group
        // with the next lower win count (within the same band).
        // If we run out of lower sub-groups within the band, cross into the
        // next lower band's highest-wins sub-group.

        for (int bandIdx = 0; bandIdx < allBandSubgroups.size(); bandIdx++) {
            TreeMap<Integer, List<RankedPlayerDTO>> winGroups = allBandSubgroups.get(bandIdx);
            List<Integer> winKeys = new ArrayList<>(winGroups.keySet()); // highest wins first

            for (int wIdx = 0; wIdx < winKeys.size(); wIdx++) {
                int winKey = winKeys.get(wIdx);
                List<RankedPlayerDTO> group = winGroups.get(winKey);

                if (group.size() % 2 == 0) continue; // already even, nothing to do

                // Find donor: next lower win sub-group in the SAME band
                if (wIdx + 1 < winKeys.size()) {
                    int donorWinKey = winKeys.get(wIdx + 1);
                    List<RankedPlayerDTO> donorGroup = winGroups.get(donorWinKey);
                    if (!donorGroup.isEmpty()) {
                        // Pull highest Elo from donor (already sorted DESC so index 0)
                        RankedPlayerDTO cascaded = donorGroup.remove(0);
                        group.add(cascaded);
                        // Re-sort receiving group by Elo DESC after new arrival
                        group.sort((a, b) -> Double.compare(elo(b), elo(a)));
                        // Remove donor group if now empty
                        if (donorGroup.isEmpty()) {
                            winGroups.remove(donorWinKey);
                            winKeys.remove(wIdx + 1);
                        }
                        continue;
                    }
                }

                // No donor within this band — cross-band cascade into next lower band
                boolean cascaded = false;
                for (int nextBandIdx = bandIdx + 1; nextBandIdx < allBandSubgroups.size(); nextBandIdx++) {
                    TreeMap<Integer, List<RankedPlayerDTO>> nextBandGroups = allBandSubgroups.get(nextBandIdx);
                    if (nextBandGroups.isEmpty()) continue;

                    // Take top-Elo player from the highest-wins sub-group of the next band
                    int highestWinKeyInNextBand = nextBandGroups.firstKey();
                    List<RankedPlayerDTO> nextDonorGroup = nextBandGroups.get(highestWinKeyInNextBand);

                    if (!nextDonorGroup.isEmpty()) {
                        RankedPlayerDTO crossCascaded = nextDonorGroup.remove(0);
                        group.add(crossCascaded);
                        group.sort((a, b) -> Double.compare(elo(b), elo(a)));
                        if (nextDonorGroup.isEmpty()) {
                            nextBandGroups.remove(highestWinKeyInNextBand);
                        }
                        cascaded = true;
                        break;
                    }
                }

                if (!cascaded) {
                    // This is the very last odd sub-group across all bands → BYE candidate
                    // Leave it odd — handled during pairing below
                }
            }

            // Clean up empty win groups
            winGroups.values().removeIf(List::isEmpty);
        }

        // ── Step 5: pair within each sub-group (top-half vs bottom-half) ──────
        List<PairingDTO> pairings = new ArrayList<>();
        int boardNumber = 1;
        int groupNumber = 1;
        RankedPlayerDTO byePlayer = null;

        for (TreeMap<Integer, List<RankedPlayerDTO>> winGroups : allBandSubgroups) {
            for (List<RankedPlayerDTO> group : winGroups.values()) {
                if (group.isEmpty()) continue;

                // Odd group at the very end = BYE
                if (group.size() % 2 != 0) {
                    // Take the lowest Elo player as the BYE candidate
                    byePlayer = group.remove(group.size() - 1);
                }

                if (group.isEmpty()) continue;

                int half = group.size() / 2;
                for (int i = 0; i < half; i++) {
                    RankedPlayerDTO p1 = group.get(i);        // higher Elo
                    RankedPlayerDTO p2 = group.get(half + i); // lower Elo

                    PairingDTO pairing = new PairingDTO();
                    pairing.setBoardNumber(boardNumber++);
                    pairing.setGroupNumber(groupNumber);
                    pairing.setBye(false);
                    pairing.setPlayer1Id(p1.getPlayerId());
                    pairing.setPlayer1Name(p1.getUsername());
                    pairing.setPlayer1Wins(p1.getTotalWins());
                    pairing.setPlayer1Rank(p1.getPlayerRank());
                    pairing.setPlayer2Id(p2.getPlayerId());
                    pairing.setPlayer2Name(p2.getUsername());
                    pairing.setPlayer2Wins(p2.getTotalWins());
                    pairing.setPlayer2Rank(p2.getPlayerRank());
                    pairings.add(pairing);
                }
                groupNumber++;
            }
        }

        // Add BYE pairing if needed
        if (byePlayer != null) {
            PairingDTO byePairing = new PairingDTO();
            byePairing.setBoardNumber(boardNumber);
            byePairing.setGroupNumber(groupNumber);
            byePairing.setBye(true);
            byePairing.setPlayer1Id(byePlayer.getPlayerId());
            byePairing.setPlayer1Name(byePlayer.getUsername());
            byePairing.setPlayer1Wins(byePlayer.getTotalWins());
            byePairing.setPlayer1Rank(byePlayer.getPlayerRank());
            byePairing.setPlayer2Id(null);
            byePairing.setPlayer2Name(null);
            byePairing.setPlayer2Wins(-1);
            byePairing.setPlayer2Rank(-1);
            pairings.add(byePairing);
        }

        return pairings;
    }

    // ── Wins-based Swiss pairing (non-mini tournaments) ───────────────────────

    private List<PairingDTO> buildWinsPairings(List<RankedPlayerDTO> rankedPlayers) {
        LinkedHashMap<Double, List<RankedPlayerDTO>> groupMap = new LinkedHashMap<>();
        for (RankedPlayerDTO player : rankedPlayers)
            groupMap.computeIfAbsent(player.getTotalWins(), k -> new ArrayList<>()).add(player);

        List<List<RankedPlayerDTO>> groups = new ArrayList<>(groupMap.values());

        for (int i = 0; i < groups.size() - 1; i++) {
            if (groups.get(i).size() % 2 != 0) {
                List<RankedPlayerDTO> next = groups.get(i + 1);
                if (!next.isEmpty()) groups.get(i).add(next.remove(0));
            }
        }
        groups.removeIf(List::isEmpty);

        RankedPlayerDTO byePlayer = null;
        List<RankedPlayerDTO> lastGroup = groups.get(groups.size() - 1);
        if (lastGroup.size() % 2 != 0)
            byePlayer = lastGroup.remove(lastGroup.size() - 1);

        List<PairingDTO> pairings = new ArrayList<>();
        int boardNumber = 1;

        for (int g = 0; g < groups.size(); g++) {
            List<RankedPlayerDTO> group = groups.get(g);
            if (group.isEmpty()) continue;
            int half = group.size() / 2;
            for (int i = 0; i < half; i++) {
                RankedPlayerDTO p1 = group.get(i);
                RankedPlayerDTO p2 = group.get(half + i);
                PairingDTO pairing = new PairingDTO();
                pairing.setBoardNumber(boardNumber++);
                pairing.setGroupNumber(g + 1);
                pairing.setBye(false);
                pairing.setPlayer1Id(p1.getPlayerId());
                pairing.setPlayer1Name(p1.getUsername());
                pairing.setPlayer1Wins(p1.getTotalWins());
                pairing.setPlayer1Rank(p1.getPlayerRank());
                pairing.setPlayer2Id(p2.getPlayerId());
                pairing.setPlayer2Name(p2.getUsername());
                pairing.setPlayer2Wins(p2.getTotalWins());
                pairing.setPlayer2Rank(p2.getPlayerRank());
                pairings.add(pairing);
            }
        }

        if (byePlayer != null) {
            PairingDTO byePairing = new PairingDTO();
            byePairing.setBoardNumber(boardNumber);
            byePairing.setGroupNumber(groups.size() + 1);
            byePairing.setBye(true);
            byePairing.setPlayer1Id(byePlayer.getPlayerId());
            byePairing.setPlayer1Name(byePlayer.getUsername());
            byePairing.setPlayer1Wins(byePlayer.getTotalWins());
            byePairing.setPlayer1Rank(byePlayer.getPlayerRank());
            byePairing.setPlayer2Id(null);
            byePairing.setPlayer2Name(null);
            byePairing.setPlayer2Wins(-1);
            byePairing.setPlayer2Rank(-1);
            pairings.add(byePairing);
        }

        return pairings;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Safe Elo getter — falls back to DEFAULT_RATING if null. */
    private double elo(RankedPlayerDTO p) {
        return p.getEloRating() != null ? p.getEloRating() : EloCalculator.DEFAULT_RATING;
    }

    // ── Inner stats helper ────────────────────────────────────────────────────

    private static class TournamentPlayerStats {
        String playerId, firstName, lastName, username;
        int    gamesPlayed = 0, cumMargin = 0, rank = 1;
        double wins = 0, avgMargin = 0;
        double eloRating = EloCalculator.DEFAULT_RATING;

        TournamentPlayerStats(String playerId, String firstName, String lastName, String username) {
            this.playerId  = playerId;
            this.firstName = firstName;
            this.lastName  = lastName;
            this.username  = username;
        }
    }
}