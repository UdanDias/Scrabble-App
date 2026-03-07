package lk.kelaniya.uok.scrabble.scrabbleapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TeamPairingDTO implements Serializable {
    private int boardNumber;
    private int groupNumber;
    private boolean bye;

    private String team1Id;
    private String team1Name;
    private double team1Wins;
    private int team1Rank;

    private String team2Id;   // null if BYE
    private String team2Name; // null if BYE
    private double team2Wins; // -1 if BYE
    private int team2Rank;    // -1 if BYE
}