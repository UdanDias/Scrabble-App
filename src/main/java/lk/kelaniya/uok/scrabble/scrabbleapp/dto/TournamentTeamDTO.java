package lk.kelaniya.uok.scrabble.scrabbleapp.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentTeamDTO {

    private String tournamentTeamId;
    private String tournamentId;
    private String teamId;
    private String teamName;
    private int    teamSize;
    private String activityStatus;
}