package lk.kelaniya.uok.scrabble.scrabbleapp.dto;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.PlayerActivityStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TournamentPlayerDTO implements Serializable {

    private String tournamentPlayerId;

    private String tournamentId;
    private String tournamentName;

    private String playerId;
    private String firstName;
    private String lastName;

    private PlayerActivityStatus activityStatus;
}