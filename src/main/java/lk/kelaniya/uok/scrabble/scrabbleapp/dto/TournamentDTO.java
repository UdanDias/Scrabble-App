package lk.kelaniya.uok.scrabble.scrabbleapp.dto;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.TournamentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TournamentDTO implements Serializable {
    private String tournamentId;
    private String tournamentName;
    private TournamentStatus status;
}