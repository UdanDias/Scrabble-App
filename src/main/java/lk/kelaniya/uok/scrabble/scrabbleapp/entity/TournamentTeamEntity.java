package lk.kelaniya.uok.scrabble.scrabbleapp.entity;

import jakarta.persistence.*;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.PlayerActivityStatus;
import lombok.*;

@Entity
@Table(name = "tournament_team")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentTeamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tournament_team_id")
    private String tournamentTeamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private TournamentEntity tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private TeamEntity team;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_status", nullable = false)
    private PlayerActivityStatus activityStatus = PlayerActivityStatus.ACTIVE;
}