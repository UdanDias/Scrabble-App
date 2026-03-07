package lk.kelaniya.uok.scrabble.scrabbleapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RankedTeamDTO implements Serializable {
    private String teamId;
    private String teamName;
    private int teamRank;
    private double totalWins;
    private int totalGamesPlayed;
    private int cumMargin;
    private double avgMargin;
    private List<TeamMemberDTO> members;
}