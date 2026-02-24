package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl.secure;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure.JWTAuthResponse;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure.SignIn;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure.UserDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.secure.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    @Override
    public JWTAuthResponse signIn(SignIn signIn) {
        return null;
    }

    @Override
    public JWTAuthResponse signUp(UserDTO userDTO) {
        return null;
    }
}
