package lk.kelaniya.uok.scrabble.scrabbleapp.util;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.GameDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.PerformanceDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PerformanceEntity;
import org.springframework.stereotype.Component;

@Component
public class PerformanceCalc {
    public String calcWinner(GameDTO gameDTO) {
        String winnerId="";
        int score1 = gameDTO.getScore1();
        int score2 = gameDTO.getScore2();

        if (score1 > score2) {
            winnerId=gameDTO.getPlayer1Id();
        }else if (score1 < score2) {
            winnerId=gameDTO.getPlayer2Id();
        }else {
            gameDTO.setGameTied(true);
        }

        return winnerId;
    }
    public int calcMargin(GameDTO gameDTO) {
        int score1 = gameDTO.getScore1();
        int score2 = gameDTO.getScore2();
        int margin;
        if (score1 > score2) {
            margin=score1-score2;
        }else{
            margin=score2-score1;
        }
        return margin;
    }
    public void updatePerformanceAfterGame(PerformanceEntity performanceEntity, GameDTO gameDTO) {
        performanceEntity.setTotalGamesPlayed(performanceEntity.getTotalGamesPlayed()+1);
        if (!gameDTO.isGameTied()){
            performanceEntity.setTotalWins(performanceEntity.getTotalWins()+1);

        }else{
            performanceEntity.setTotalWins(performanceEntity.getTotalWins()+0.5);
        }

    }
}
