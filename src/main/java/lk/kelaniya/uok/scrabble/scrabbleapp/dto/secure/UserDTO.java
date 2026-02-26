package lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure;

import com.fasterxml.jackson.annotation.JsonFormat;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

//
//import lk.kelaniya.uok.scrabble.scrabbleapp.dto.Role;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//import java.io.Serializable;
//
//@AllArgsConstructor
//@NoArgsConstructor
//@Data
//public class UserDTO implements Serializable {
//    private String userId;
//    private String firstName;
//    private String lastName;
//    private String email;
//    private String password;
//    private Role role;
//    private  String playerId;
//}
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserDTO implements Serializable {
    // User fields
    private String userId;
    private String email;
    private String password;
    private Role role;

    // Player fields
    private String playerId;
    private String firstName;
    private String lastName;
    private int age;
    private String gender;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;
    private String phone;
    private String address;
    private String faculty;
    private String academicLevel;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate accountCreatedDate;
}
