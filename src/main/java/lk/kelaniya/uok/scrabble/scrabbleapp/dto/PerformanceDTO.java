package lk.kelaniya.uok.scrabble.scrabbleapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PerformanceDTO implements Serializable {
    private String performanceId;
    private String playerId;
    private double totalWins;
    private int totalGamesPlayed;
    private int cumMargin;
    private double avgMargin;
    private int playerRank;
}
