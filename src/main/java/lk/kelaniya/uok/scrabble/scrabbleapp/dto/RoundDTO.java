package lk.kelaniya.uok.scrabble.scrabbleapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RoundDTO implements Serializable {

    private String roundId;
    private int roundNumber;
    private boolean completed;       // ← NEW: frontend sends true when admin marks round complete
    private String tournamentId;
}