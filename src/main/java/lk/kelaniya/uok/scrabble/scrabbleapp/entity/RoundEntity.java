package lk.kelaniya.uok.scrabble.scrabbleapp.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "round")
@Data
public class RoundEntity {

    @Id
    @Column(name = "round_id")
    private String roundId;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "completed", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean completed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private TournamentEntity tournament;

    // ── Cascade deletes to all games in this round ────────────────────────────
    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameEntity> games;
}