package lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserDTO implements Serializable {
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Role role;
}
