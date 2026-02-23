package lk.kelaniya.uok.scrabble.scrabbleapp.service;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.GameDTO;

import java.time.LocalDate;
import java.util.List;

public interface GameService {
    void addGame(GameDTO gameDTO);
    void addByeGame(String playerId, LocalDate gameDate);
    void deleteGame(String gameId);
    GameDTO updateGame(String gameId,GameDTO gameDTO);
    GameDTO getSelectedGame(String gameId);
    List<GameDTO> getAllGames();
}
