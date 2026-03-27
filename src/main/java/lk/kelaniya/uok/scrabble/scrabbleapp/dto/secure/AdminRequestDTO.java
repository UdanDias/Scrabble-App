package lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminRequestDTO {
    private String requestId;
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private RequestStatus status;
    private LocalDate requestedDate;
    private LocalDate resolvedDate;
}