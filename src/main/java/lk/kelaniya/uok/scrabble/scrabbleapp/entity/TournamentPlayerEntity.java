package lk.kelaniya.uok.scrabble.scrabbleapp.entity;

import jakarta.persistence.*;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.PlayerActivityStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
        name = "tournament_player",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tournament_id", "player_id"})
)
public class TournamentPlayerEntity {

    @Id
    private String tournamentPlayerId;

    @Column(name = "tournament_id", nullable = false)
    private String tournamentId;

    @Column(nullable = false)
    private String tournamentName;

    @Column(name = "player_id", nullable = false)
    private String playerId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerActivityStatus activityStatus = PlayerActivityStatus.ACTIVE;
    @Column(nullable = false)
    private int registeredFromRoundNumber = 1;
    // ← NO @ManyToOne relationships here at all
}