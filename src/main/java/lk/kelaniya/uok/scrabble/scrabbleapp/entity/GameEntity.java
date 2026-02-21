package lk.kelaniya.uok.scrabble.scrabbleapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name="game")
public class GameEntity {
    @Id
    private String gameId;

    @ManyToOne
    @JoinColumn(name = "player1_id", nullable = true)
    private PlayerEntity player1;

    @ManyToOne
    @JoinColumn(name = "player2_id", nullable = true)
    private PlayerEntity player2;

    private int score1;
    private int score2;
    private int margin;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private PlayerEntity winner;

    private LocalDate gameDate;
    private boolean bye = false;
}
