package lk.kelaniya.uok.scrabble.scrabbleapp.service.secure;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure.JWTAuthResponse;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure.SignIn;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure.UserDTO;

public interface AuthService {
    JWTAuthResponse signIn(SignIn signIn);
    JWTAuthResponse signUp(UserDTO userDTO);
}
