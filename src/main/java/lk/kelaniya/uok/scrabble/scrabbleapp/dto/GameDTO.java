package lk.kelaniya.uok.scrabble.scrabbleapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.PrivateKey;
import java.time.LocalDate;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class GameDTO {
    private String gameId;
    private String player1Id;
    private String player2Id;
    private int score1;
    private int score2;
    private int margin ;
    private String winnerId;
    private LocalDate gameDate;

}
