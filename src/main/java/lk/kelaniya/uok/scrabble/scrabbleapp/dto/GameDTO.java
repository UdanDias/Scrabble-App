package lk.kelaniya.uok.scrabble.scrabbleapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class GameDTO implements Serializable {
    private String gameId;
    private String player1Id;
    private String player2Id;
    private int score1;
    private int score2;
    private boolean gameTied =false;
    private int margin ;
    private String winnerId;
    private LocalDate gameDate;
    private boolean bye = false;

}
