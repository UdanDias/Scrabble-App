package lk.kelaniya.uok.scrabble.scrabbleapp.service.secure;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure.JWTAuthResponse;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure.SignIn;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure.UserDTO;

import java.util.List;

public interface AuthService {
    JWTAuthResponse signIn(SignIn signIn);
    JWTAuthResponse signUp(UserDTO userDTO);
    UserDTO getSelectedUserById(String userId);
    List<UserDTO> getAllUsers();
    UserDTO updateUser(String userId, UserDTO userDTO);
    void deleteUser(String userId);
}
