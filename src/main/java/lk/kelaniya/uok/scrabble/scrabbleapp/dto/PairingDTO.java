package lk.kelaniya.uok.scrabble.scrabbleapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PairingDTO implements Serializable {
    private int boardNumber;
    private String player1Id;
    private String player1Name;
    private double player1Wins;
    private int player1Rank;

    private String player2Id;   // null if BYE
    private String player2Name; // null if BYE
    private double player2Wins; // -1 if BYE
    private int player2Rank;    // -1 if BYE

    private boolean bye;
    private int groupNumber;
}