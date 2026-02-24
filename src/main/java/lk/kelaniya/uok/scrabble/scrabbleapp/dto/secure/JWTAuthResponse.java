package lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder

public class JWTAuthResponse  {
    private String token;
}
