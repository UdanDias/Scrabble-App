package lk.kelaniya.uok.scrabble.scrabbleapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class ByeGameDTO implements Serializable {
    private String playerId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate gameDate;
    private String roundId;
    private int margin;

}
