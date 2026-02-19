package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.PerformanceDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.GameDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PerformanceDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PerformanceEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.InputMarginIncorrectException;
import lk.kelaniya.uok.scrabble.scrabbleapp.exception.PlayerNotFoundException;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.GameService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.PerformanceCalc;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.UtilData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;



import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional


public class GameServiceImpl implements GameService {

    private final PerformanceCalc performanceCalc;
    private final PerformanceDao performanceDao;
    @Override
    public void addGame(GameDTO gameDTO)  {
        gameDTO.setGameId(UtilData.generateGameId());
        int calcMargin= performanceCalc.calcMargin(gameDTO);
        gameDTO.setMargin(calcMargin);
        gameDTO.setWinnerId(performanceCalc.calcWinner(gameDTO));

        PerformanceEntity player1Perf=performanceDao.findById(gameDTO.getPlayer1Id())
                .orElseThrow(()->new PlayerNotFoundException("Player1 not found"));

        PerformanceEntity player2Perf=performanceDao.findById(gameDTO.getPlayer2Id())
                .orElseThrow(()->new PlayerNotFoundException("Player2 not found"));

        if (!gameDTO.isGameTied()){
//            PerformanceEntity winner=performanceDao.findById(gameDTO.getWinnerId())
//                    .orElseThrow(()->new PlayerNotFoundException("Player not found"));
            if(gameDTO.getWinnerId().equals(gameDTO.getPlayer1Id())){
                performanceCalc.updatePerformanceAfterGame(player1Perf,gameDTO);
                player2Perf.setTotalGamesPlayed(player2Perf.getTotalGamesPlayed()+1);

            }else{
                performanceCalc.updatePerformanceAfterGame(player2Perf,gameDTO);
                player1Perf.setTotalGamesPlayed(player1Perf.getTotalGamesPlayed()+1);
            }
        }else{

            performanceCalc.updatePerformanceAfterGame(player1Perf,gameDTO);
            performanceCalc.updatePerformanceAfterGame(player2Perf,gameDTO);
        }


    }

    @Override
    public void deleteGame(String gameId) {

    }

    @Override
    public void updateGame(String gameId, GameDTO gameDTO) {

    }

    @Override
    public GameDTO getSelectedGame(String gameId) {
        return null;
    }

    @Override
    public List<GameDTO> getAllGames() {
        return List.of();
    }
}
