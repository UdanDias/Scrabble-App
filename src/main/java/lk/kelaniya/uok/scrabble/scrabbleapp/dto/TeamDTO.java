package lk.kelaniya.uok.scrabble.scrabbleapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TeamDTO implements Serializable {
    private String teamId;
    private String teamName;
    private int teamSize;
    private List<String> playerIds;       // for create/update requests
    private List<TeamMemberDTO> members;  // for responses (populated with names)
}