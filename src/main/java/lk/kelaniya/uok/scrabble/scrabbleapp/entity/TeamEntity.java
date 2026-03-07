package lk.kelaniya.uok.scrabble.scrabbleapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "team")
public class TeamEntity {

    @Id
    private String teamId;

    private String teamName;

    private int teamSize; // predefined max players

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "team_players",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    private List<PlayerEntity> players;
}