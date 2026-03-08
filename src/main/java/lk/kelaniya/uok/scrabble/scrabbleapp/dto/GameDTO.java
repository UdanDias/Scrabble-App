package lk.kelaniya.uok.scrabble.scrabbleapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private int margin ;
    @JsonProperty("isgameTied")
    private boolean gameTied =false;
    private String winnerId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate gameDate;
    @JsonProperty("isByeGame")
    private boolean bye = false;
    private String roundId;


}
