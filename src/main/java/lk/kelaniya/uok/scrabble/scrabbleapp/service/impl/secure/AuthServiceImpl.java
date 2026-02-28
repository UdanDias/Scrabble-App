package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl.secure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.PerformanceDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.PlayerDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.secure.UserDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure.JWTAuthResponse;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure.SignIn;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure.UserDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PerformanceEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PlayerEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.security.UserEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.security.jwt.JWTutils;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.secure.AuthService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.EntityDTOConvert;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.PerformanceCalc;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.UtilData;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserDao userDao;
    private final JWTutils jwtutils;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final PlayerDao playerDao;
    private final EntityDTOConvert entityDTOConvert;
    private final PerformanceCalc performanceCalc;

    // ✅ Add separately with @PersistenceContext
    @PersistenceContext
    private EntityManager entityManager;  // not final


@Override
public JWTAuthResponse signIn(SignIn signIn) {
    authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(signIn.getEmail(), signIn.getPassword()));
    var userByEmail = userDao.findByEmail(signIn.getEmail())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    String playerId = userByEmail.getPlayer() != null
            ? userByEmail.getPlayer().getPlayerId()
            : null;

    var generateToken = jwtutils.generateToken(
            userByEmail.getEmail(),
            userByEmail.getAuthorities(),
            playerId);
    return JWTAuthResponse.builder().token(generateToken).build();
}


    @Override
    public JWTAuthResponse signUp(UserDTO registerDTO) {
        // Create player
        PlayerEntity player = new PlayerEntity();
        player.setPlayerId(UtilData.generatePlayerId());
        player.setFirstName(registerDTO.getFirstName());
        player.setLastName(registerDTO.getLastName());
        player.setAge(registerDTO.getAge());
        player.setGender(registerDTO.getGender());
        player.setDob(registerDTO.getDob());
        player.setEmail(registerDTO.getEmail());
        player.setPhone(registerDTO.getPhone());
        player.setAddress(registerDTO.getAddress());
        player.setFaculty(registerDTO.getFaculty());
        player.setAcademicLevel(registerDTO.getAcademicLevel());
        player.setAccountCreatedDate(LocalDate.now());
        player.setDeleted(false);

        // Create performance and link
        PerformanceEntity performance = new PerformanceEntity();
        performance.setPlayerId(player.getPlayerId());
        performance.setPlayer(player);
        performance.setTotalWins(0.0);
        performance.setTotalGamesPlayed(0);
        performance.setCumMargin(0);
        performance.setAvgMargin(0.0);
        performance.setPlayerRank(0);
        player.setPerformance(performance);

        // Use persist instead of save/merge ✅
        entityManager.persist(player);
        entityManager.flush();

        performanceCalc.updateRanks();
        // Create user
        UserEntity userEntity = new UserEntity();
        userEntity.setUserId(UtilData.generateUserId());
        userEntity.setFirstName(registerDTO.getFirstName());
        userEntity.setLastName(registerDTO.getLastName());
        userEntity.setEmail(registerDTO.getEmail());
        userEntity.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        userEntity.setRole(registerDTO.getRole());
        userEntity.setPlayer(player);
        UserEntity savedUser = userDao.save(userEntity);

        var generatedToken = jwtutils.generateToken(
                savedUser.getEmail(),
                savedUser.getAuthorities(),
                savedUser.getPlayer().getPlayerId());
        return JWTAuthResponse.builder().token(generatedToken).build();
    }

    @Override
    public UserDTO getSelectedUserById(String userId) {
        UserEntity user = userDao.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
        return entityDTOConvert.convertUserEntityToUserDTO(user);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return entityDTOConvert.convertUserEntityListToUserDTOList(userDao.findAll());
    }

    @Override
    public UserDTO updateUser(String userId, UserDTO userDTO) {
        UserEntity user = userDao.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setRole(userDTO.getRole());

        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            // Only encode and save if the new password is different from the current one
            if (!passwordEncoder.matches(userDTO.getPassword(), user.getPassword())) {
                user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            }
        }

        PlayerEntity player = user.getPlayer();
        if (player != null) {
            player.setFirstName(userDTO.getFirstName());
            player.setLastName(userDTO.getLastName());
            player.setAge(userDTO.getAge());
            player.setGender(userDTO.getGender());
            player.setDob(userDTO.getDob());
            player.setEmail(userDTO.getEmail());
            player.setPhone(userDTO.getPhone());
            player.setAddress(userDTO.getAddress());
            player.setFaculty(userDTO.getFaculty());
            player.setAcademicLevel(userDTO.getAcademicLevel());
            playerDao.save(player);
        }

        UserEntity updatedUser = userDao.save(user);
        return entityDTOConvert.convertUserEntityToUserDTO(updatedUser);
    }

    @Override
    public void deleteUser(String userId) {
        UserEntity user = userDao.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        PlayerEntity player = user.getPlayer();

        // Delete user first to remove the FK reference to player
        userDao.delete(user);
        userDao.flush();

        if (player != null) {
            player.setDeleted(true);
            player.setDeletedDate(LocalDate.now());
            playerDao.save(player);
        }
    }
}
