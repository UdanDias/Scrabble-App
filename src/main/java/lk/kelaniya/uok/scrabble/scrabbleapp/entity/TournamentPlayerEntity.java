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
    private String tournamentPlayerId;          // generated UUID / custom ID

    // ── Denormalised columns (as requested) ──────────────────────────────────
    @Column(nullable = false)
    private String tournamentId;

    @Column(nullable = false)
    private String tournamentName;

    @Column(nullable = false)
    private String playerId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    // ── Activity status ───────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerActivityStatus activityStatus = PlayerActivityStatus.ACTIVE;

    // ── JPA relationships (for queries; not returned directly to frontend) ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", insertable = false, updatable = false)
    private TournamentEntity tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", insertable = false, updatable = false)
    private PlayerEntity player;
}