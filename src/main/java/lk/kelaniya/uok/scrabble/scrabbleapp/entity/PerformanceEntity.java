package lk.kelaniya.uok.scrabble.scrabbleapp.entity;

import jakarta.persistence.*;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.EloCalculator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "performance")
public class PerformanceEntity {

    @Id
    private String playerId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "player_id", nullable = false, unique = true)
    @NotFound(action = NotFoundAction.IGNORE)
    @ToString.Exclude
    private PlayerEntity player;

    private Double totalWins;
    private Integer totalGamesPlayed;
    private Integer cumMargin;
    private Double avgMargin;
    private Integer playerRank;

    // ✅ Elo rating — used for Mini Tournament Uok ranking
    // Default 1200.0, reset and replayed on every recalculation
    @Column(name = "elo_rating", nullable = false)
    private Double eloRating = EloCalculator.DEFAULT_RATING;
}