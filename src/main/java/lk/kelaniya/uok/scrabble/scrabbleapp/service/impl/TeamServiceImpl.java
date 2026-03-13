package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.GameDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.PlayerDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.TeamDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.RankedTeamDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TeamDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TeamMemberDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.TeamPairingDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.GameEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PlayerEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.TeamEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.TeamService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.UtilData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamDao teamDao;
    private final PlayerDao playerDao;
    private final GameDao gameDao;

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Override
    public TeamDTO createTeam(TeamDTO teamDTO) {
        TeamEntity entity = new TeamEntity();
        entity.setTeamId(UtilData.generateTeamId());
        entity.setTeamName(teamDTO.getTeamName());
        entity.setTeamSize(teamDTO.getTeamSize());
        entity.setPlayers(resolvePlayers(teamDTO.getPlayerIds()));
        return toDTO(teamDao.save(entity));
    }

    @Override
    public TeamDTO getTeam(String teamId) {
        TeamEntity entity = teamDao.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found: " + teamId));
        return toDTO(entity);
    }

    @Override
    public List<TeamDTO> getAllTeams() {
        return teamDao.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public TeamDTO updateTeam(String teamId, TeamDTO teamDTO) {
        TeamEntity entity = teamDao.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found: " + teamId));
        entity.setTeamName(teamDTO.getTeamName());
        entity.setTeamSize(teamDTO.getTeamSize());
        if (teamDTO.getPlayerIds() != null) {
            entity.setPlayers(resolvePlayers(teamDTO.getPlayerIds()));
        }
        return toDTO(teamDao.save(entity));
    }

    @Override
    public void deleteTeam(String teamId) {
        TeamEntity entity = teamDao.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found: " + teamId));
        teamDao.delete(entity);
    }

    // ── Team Leaderboard ─────────────────────────────────────────────────────

    @Override
    public List<RankedTeamDTO> getTeamLeaderboard(String tournamentId) {

        List<TeamEntity> teams = teamDao.findAll();
        if (teams.isEmpty()) return List.of();

        List<GameEntity> tournamentGames = gameDao.getGamesByTournamentId(tournamentId);
        if (tournamentGames.isEmpty()) return List.of();

        // ── player → team lookup ─────────────────────────────
        Map<String,String> playerToTeam = new HashMap<>();
        for (TeamEntity team : teams) {
            if (team.getPlayers() != null) {
                for (PlayerEntity p : team.getPlayers()) {
                    playerToTeam.put(p.getPlayerId(), team.getTeamId());
                }
            }
        }

        // ── initialise stats ─────────────────────────────────
        Map<String,TeamStats> statsMap = new HashMap<>();
        for (TeamEntity team : teams) {
            statsMap.put(team.getTeamId(),
                    new TeamStats(team.getTeamId(), team.getTeamName()));
        }

        // ── group games by round ─────────────────────────────
        Map<String,List<GameEntity>> gamesByRound = new LinkedHashMap<>();
        for (GameEntity g : tournamentGames) {

            String roundId = g.getRound() != null
                    ? g.getRound().getRoundId()
                    : "NO_ROUND";

            gamesByRound
                    .computeIfAbsent(roundId,k->new ArrayList<>())
                    .add(g);
        }

        // ── process each round ───────────────────────────────
        for (Map.Entry<String,List<GameEntity>> entry : gamesByRound.entrySet()) {

            List<GameEntity> roundGames = entry.getValue();

            Map<String,Double> roundGameWins = new HashMap<>();
            Map<String,Integer> roundMargin  = new HashMap<>();

            for (GameEntity game : roundGames) {

                if (game.isBye()) {

                    if (game.getPlayer1()==null) continue;

                    String tid = playerToTeam.get(game.getPlayer1().getPlayerId());
                    if (tid==null) continue;

                    TeamStats stats = statsMap.get(tid);

                    stats.totalGamesPlayed++;
                    stats.cumMargin += 50;

                    roundGameWins.merge(tid,1.0,Double::sum);
                    roundMargin.merge(tid,50,Integer::sum);

                    continue;
                }

                if (game.getPlayer1()==null || game.getPlayer2()==null) continue;

                String p1id = game.getPlayer1().getPlayerId();
                String p2id = game.getPlayer2().getPlayerId();

                String t1id = playerToTeam.get(p1id);
                String t2id = playerToTeam.get(p2id);

                int margin1 = game.getScore1() - game.getScore2();
                int margin2 = game.getScore2() - game.getScore1();

                if (t1id!=null) {

                    TeamStats t1 = statsMap.get(t1id);

                    t1.totalGamesPlayed++;
                    t1.cumMargin += margin1;

                    roundMargin.merge(t1id,margin1,Integer::sum);

                    if (game.isGameTied()) {
                        roundGameWins.merge(t1id,0.5,Double::sum);
                    }
                    else if (game.getWinner()!=null &&
                            game.getWinner().getPlayerId().equals(p1id)) {

                        roundGameWins.merge(t1id,1.0,Double::sum);
                    }
                }

                if (t2id!=null && !Objects.equals(t1id,t2id)) {

                    TeamStats t2 = statsMap.get(t2id);

                    t2.totalGamesPlayed++;
                    t2.cumMargin += margin2;

                    roundMargin.merge(t2id,margin2,Integer::sum);

                    if (game.isGameTied()) {
                        roundGameWins.merge(t2id,0.5,Double::sum);
                    }
                    else if (game.getWinner()!=null &&
                            game.getWinner().getPlayerId().equals(p2id)) {

                        roundGameWins.merge(t2id,1.0,Double::sum);
                    }
                }
            }

            // ── determine round winner ───────────────────────
            for (Map.Entry<String,Double> e : roundGameWins.entrySet()) {

                if (e.getValue() > 2.5) {

                    TeamStats stats = statsMap.get(e.getKey());
                    if (stats!=null) stats.roundWins++;
                }
            }

            // ── handle 2.5 vs 2.5 tie ───────────────────────
            List<String> roundTeams = new ArrayList<>(roundGameWins.keySet());

            if (roundTeams.size()==2) {

                String t1 = roundTeams.get(0);
                String t2 = roundTeams.get(1);

                double w1 = roundGameWins.getOrDefault(t1,0.0);
                double w2 = roundGameWins.getOrDefault(t2,0.0);

                if (w1==2.5 && w2==2.5) {

                    int m1 = roundMargin.getOrDefault(t1,0);
                    int m2 = roundMargin.getOrDefault(t2,0);

                    if (m1 > m2) {
                        statsMap.get(t1).roundWins++;
                    }
                    else if (m2 > m1) {
                        statsMap.get(t2).roundWins++;
                    }
                }
            }
        }

        // ── filter active teams ─────────────────────────────
        List<TeamStats> active = statsMap.values()
                .stream()
                .filter(s -> s.totalGamesPlayed>0)
                .collect(Collectors.toList());

        // ── sort leaderboard ───────────────────────────────
        active.sort((a,b)->{

            int c = Integer.compare(b.roundWins,a.roundWins);
            if (c!=0) return c;

            return Integer.compare(b.cumMargin,a.cumMargin);
        });

        // ── assign ranks ───────────────────────────────────
        int rank = 1;

        for (int i=0;i<active.size();i++) {

            if (i>0) {

                TeamStats prev = active.get(i-1);
                TeamStats curr = active.get(i);

                if (curr.roundWins!=prev.roundWins ||
                        curr.cumMargin!=prev.cumMargin) {

                    rank = i+1;
                }
            }

            active.get(i).rank = rank;
        }

        Map<String,TeamEntity> teamEntityMap = teams.stream()
                .collect(Collectors.toMap(TeamEntity::getTeamId,t->t));

        return active.stream().map(s->{

            RankedTeamDTO dto = new RankedTeamDTO();

            dto.setTeamId(s.teamId);
            dto.setTeamName(s.teamName);
            dto.setTeamRank(s.rank);
            dto.setRoundWins(s.roundWins);
            dto.setTotalWins(s.roundWins);
            dto.setTotalGamesPlayed(s.totalGamesPlayed);
            dto.setCumMargin(s.cumMargin);

            dto.setAvgMargin(
                    s.totalGamesPlayed>0
                            ? Math.round((double)s.cumMargin/s.totalGamesPlayed*100.0)/100.0
                            : 0.0
            );

            TeamEntity te = teamEntityMap.get(s.teamId);

            if (te!=null && te.getPlayers()!=null) {

                dto.setMembers(
                        te.getPlayers()
                                .stream()
                                .map(p->new TeamMemberDTO(
                                        p.getPlayerId(),
                                        p.getFirstName(),
                                        p.getLastName()))
                                .collect(Collectors.toList())
                );
            }

            return dto;

        }).collect(Collectors.toList());
    }

    // ── Team Swiss Pairings ───────────────────────────────────────────────────

    @Override
    public List<TeamPairingDTO> getTeamSwissPairings(String tournamentId) {
        List<RankedTeamDTO> rankedTeams = getTeamLeaderboard(tournamentId);
        if (rankedTeams.isEmpty()) return List.of();

        LinkedHashMap<Double, List<RankedTeamDTO>> groupMap = new LinkedHashMap<>();
        for (RankedTeamDTO team : rankedTeams) {
            groupMap.computeIfAbsent(team.getTotalWins(), k -> new ArrayList<>()).add(team);
        }

        List<List<RankedTeamDTO>> groups = new ArrayList<>(groupMap.values());

        for (int i = 0; i < groups.size() - 1; i++) {
            if (groups.get(i).size() % 2 != 0) {
                List<RankedTeamDTO> next = groups.get(i + 1);
                if (!next.isEmpty()) {
                    groups.get(i).add(next.remove(0));
                }
            }
        }
        groups.removeIf(List::isEmpty);

        RankedTeamDTO byeTeam = null;
        List<RankedTeamDTO> lastGroup = groups.get(groups.size() - 1);
        if (lastGroup.size() % 2 != 0) {
            byeTeam = lastGroup.remove(lastGroup.size() - 1);
        }

        List<TeamPairingDTO> pairings = new ArrayList<>();
        int boardNumber = 1;

        for (int g = 0; g < groups.size(); g++) {
            List<RankedTeamDTO> group = groups.get(g);
            if (group.isEmpty()) continue;
            int half = group.size() / 2;
            List<RankedTeamDTO> top    = group.subList(0, half);
            List<RankedTeamDTO> bottom = group.subList(half, group.size());

            for (int i = 0; i < half; i++) {
                RankedTeamDTO t1 = top.get(i);
                RankedTeamDTO t2 = bottom.get(i);
                TeamPairingDTO p = new TeamPairingDTO();
                p.setBoardNumber(boardNumber++);
                p.setGroupNumber(g + 1);
                p.setBye(false);
                p.setTeam1Id(t1.getTeamId());
                p.setTeam1Name(t1.getTeamName());
                p.setTeam1Wins(t1.getTotalWins());
                p.setTeam1Rank(t1.getTeamRank());
                p.setTeam2Id(t2.getTeamId());
                p.setTeam2Name(t2.getTeamName());
                p.setTeam2Wins(t2.getTotalWins());
                p.setTeam2Rank(t2.getTeamRank());
                pairings.add(p);
            }
        }

        if (byeTeam != null) {
            TeamPairingDTO byePairing = new TeamPairingDTO();
            byePairing.setBoardNumber(boardNumber);
            byePairing.setGroupNumber(groups.size() + 1);
            byePairing.setBye(true);
            byePairing.setTeam1Id(byeTeam.getTeamId());
            byePairing.setTeam1Name(byeTeam.getTeamName());
            byePairing.setTeam1Wins(byeTeam.getTotalWins());
            byePairing.setTeam1Rank(byeTeam.getTeamRank());
            byePairing.setTeam2Id(null);
            byePairing.setTeam2Name(null);
            byePairing.setTeam2Wins(-1);
            byePairing.setTeam2Rank(-1);
            pairings.add(byePairing);
        }

        return pairings;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<PlayerEntity> resolvePlayers(List<String> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) return new ArrayList<>();
        return playerIds.stream()
                .map(id -> playerDao.findById(id)
                        .orElseThrow(() -> new RuntimeException("Player not found: " + id)))
                .collect(Collectors.toList());
    }

    private TeamDTO toDTO(TeamEntity entity) {
        TeamDTO dto = new TeamDTO();
        dto.setTeamId(entity.getTeamId());
        dto.setTeamName(entity.getTeamName());
        dto.setTeamSize(entity.getTeamSize());
        if (entity.getPlayers() != null) {
            dto.setPlayerIds(entity.getPlayers().stream()
                    .map(PlayerEntity::getPlayerId).collect(Collectors.toList()));
            dto.setMembers(entity.getPlayers().stream()
                    .map(p -> new TeamMemberDTO(p.getPlayerId(), p.getFirstName(), p.getLastName()))
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    // ── Inner stats class ─────────────────────────────────────────────────────

    private static class TeamStats {
        String teamId, teamName;
        int totalGamesPlayed = 0;
        int cumMargin        = 0;
        int roundWins        = 0;
        int rank             = 1;
        double avgMargin     = 0;

        TeamStats(String teamId, String teamName) {
            this.teamId   = teamId;
            this.teamName = teamName;
        }
    }
}