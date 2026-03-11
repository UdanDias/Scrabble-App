package lk.kelaniya.uok.scrabble.scrabbleapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PlayerDTO implements Serializable {
    private String playerId;
    private String firstName;
    private String lastName;
    private int age;
    private String gender;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;
    private String email;
    private String phone;
    private String address;
    private String faculty;
    private String academicLevel;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate accountCreatedDate;
    private String username;




}
