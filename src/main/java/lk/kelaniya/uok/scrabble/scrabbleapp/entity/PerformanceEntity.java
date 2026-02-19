package lk.kelaniya.uok.scrabble.scrabbleapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "performance")
public class PerformanceEntity {

    @Id
    private String performanceId;

    @OneToOne
    @JoinColumn(name = "player_id", unique = true)
    private PlayerEntity player;

    private double totalWins;
    private int totalGamesPlayed;
    private int cumMargin;
    private double avgMargin;
    private int playerRank;
}
