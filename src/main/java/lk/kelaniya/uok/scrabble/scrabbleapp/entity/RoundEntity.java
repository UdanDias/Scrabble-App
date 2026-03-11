//package lk.kelaniya.uok.scrabble.scrabbleapp.entity;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.util.List;
//
//@AllArgsConstructor
//@NoArgsConstructor
//@Data
//@Entity
//@Table(name = "round")
//public class RoundEntity {
//
//    @Id
//    private String roundId;
//
//    private int roundNumber;
//
//    // ── NEW: admin marks this true when all games for the round are entered ──
//    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
//    private boolean completed = false;
//
//    @ManyToOne(optional = false)
//    @JoinColumn(name = "tournament_id", nullable = false)
//    private TournamentEntity tournament;
//
//    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<GameEntity> games;
//}
package lk.kelaniya.uok.scrabble.scrabbleapp.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "round")
@Data
public class RoundEntity {

    @Id
    @Column(name = "round_id")
    private String roundId;

    @Column(name = "round_number")
    private Integer roundNumber;

    // ✅ NEW — persisted flag; set to true when the round is officially completed
    @Column(name = "completed", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean completed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private TournamentEntity tournament;
}