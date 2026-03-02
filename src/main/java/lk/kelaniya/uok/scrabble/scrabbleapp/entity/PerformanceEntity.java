package lk.kelaniya.uok.scrabble.scrabbleapp.entity;

import jakarta.persistence.*;
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
    @JoinColumn(name = "player_id",nullable = false, unique = true)
    @NotFound(action = NotFoundAction.IGNORE)
    @ToString.Exclude
    private PlayerEntity player;

    private Double totalWins;
    private Integer totalGamesPlayed;
    private Integer cumMargin;
    private Double avgMargin;
    private Integer playerRank;
}
