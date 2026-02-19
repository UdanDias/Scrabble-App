package lk.kelaniya.uok.scrabble.scrabbleapp.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "player")
public class PlayerEntity {
    @Id
    private String playerId;

    private String firstName;
    private String lastName;
    private int age;
    private String gender;
    private LocalDate dob;
    private String email;
    private String phone;
    private String address;
    private String faculty;
    private String academicLevel;
    private LocalDate accountCreatedDate;

    @OneToOne(mappedBy = "player")
    private PerformanceEntity performance;


    @OneToMany(mappedBy = "player1")
    private List<GameEntity> gamesAsPlayer1;

    @OneToMany(mappedBy = "player2")
    private List<GameEntity> gamesAsPlayer2;

    @OneToMany(mappedBy = "winner")
    private List<GameEntity> gamesWon;
}
