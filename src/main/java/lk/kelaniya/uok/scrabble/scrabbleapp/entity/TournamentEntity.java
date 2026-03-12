package lk.kelaniya.uok.scrabble.scrabbleapp.entity;

import jakarta.persistence.*;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.TournamentStatus;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.TournamentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "tournament")
public class TournamentEntity {

    @Id
    private String tournamentId;
    private String tournamentName;

    @Enumerated(EnumType.STRING)
    private TournamentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tournament_type")
    private TournamentType tournamentType;

    // Rounds → Games (double cascade via RoundEntity)
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoundEntity> rounds;

    // Players registered to this tournament
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentPlayerEntity> tournamentPlayers;

    // Teams registered to this tournament
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentTeamEntity> tournamentTeams;
}