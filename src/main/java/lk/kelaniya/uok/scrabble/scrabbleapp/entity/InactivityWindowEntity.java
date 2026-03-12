package lk.kelaniya.uok.scrabble.scrabbleapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inactivity_window")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InactivityWindowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "window_id")
    private String windowId;

    // The player this window belongs to
    @Column(name = "player_id", nullable = false)
    private String playerId;

    // The tournament this window belongs to
    @Column(name = "tournament_id", nullable = false)
    private String tournamentId;

    /**
     * The first round of the 3-consecutive-miss streak.
     * e.g. if rounds 3,4,5 were missed and detected at round 5,
     * fromRound = 3 (not 5).
     */
    @Column(name = "from_round", nullable = false)
    private int fromRound;

    /**
     * The round the player actually came back and played.
     * null means the player is still inactive.
     * e.g. if they return at round 9, returnRound = 9.
     * The freeze covers [fromRound, returnRound) — round 9 itself is NOT frozen.
     */
    @Column(name = "return_round")
    private Integer returnRound;
}