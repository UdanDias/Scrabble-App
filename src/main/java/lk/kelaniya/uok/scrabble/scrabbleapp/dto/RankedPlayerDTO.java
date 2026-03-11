//package lk.kelaniya.uok.scrabble.scrabbleapp.dto;
//
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.io.Serializable;
//
//@AllArgsConstructor
//@NoArgsConstructor
//@Data
//public class RankedPlayerDTO implements Serializable {
//    private String playerId;
//    private String firstName;
//    private String lastName;
//    private Integer playerRank;
//    private Double totalWins;
//    private Integer totalGamesPlayed;
//    private Double avgMargin;
//    private Integer cumMargin;
//    private String username;
//    private Double eloRating; // ✅ added — only meaningful for Mini Tournament Uok
//    private boolean provisional;
//    private Double previousEloRating;
//}
package lk.kelaniya.uok.scrabble.scrabbleapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RankedPlayerDTO implements Serializable {
    private String playerId;
    private String firstName;
    private String lastName;
    private Integer playerRank;
    private Double totalWins;
    private Integer totalGamesPlayed;
    private Double avgMargin;
    private Integer cumMargin;
    private String username;
    private Double eloRating;         // ✅ only meaningful for Mini Tournament Uok
    private Double previousEloRating; // ✅ Elo before last completed round's games
    private Boolean provisional;      // ✅ true if gamesPlayed < PROVISIONAL_THRESHOLD
}