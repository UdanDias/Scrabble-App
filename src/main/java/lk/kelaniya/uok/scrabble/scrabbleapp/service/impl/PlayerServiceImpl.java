package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PlayerDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.PlayerService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.UtilData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional

public class PlayerServiceImpl implements PlayerService {
    @Override
    public void addPlayer(PlayerDTO playerDTO) {
        playerDTO.setPlayerId(UtilData.generatePlayerId());
        playerDTO.setAge(UtilData.calcAge(playerDTO.getDob()));
        playerDTO.setAccountCreatedDate(UtilData.generateTodayDate());

    }

    @Override
    public void deletePlayer(String playerId) {

    }

    @Override
    public void updatePlayer(String playerId, PlayerDTO playerDTO) {

    }

    @Override
    public PlayerDTO getSelectedPlayer(String playerId) {
        return null;
    }

    @Override
    public List<PlayerDTO> getAllPlayers() {
        return List.of();
    }
}
