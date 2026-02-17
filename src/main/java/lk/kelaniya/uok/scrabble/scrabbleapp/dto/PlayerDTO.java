package lk.kelaniya.uok.scrabble.scrabbleapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PlayerDTO {
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
    private float totalWins;
    private int cumMargin;
    private float avgMargin;
    private int rank;



}
