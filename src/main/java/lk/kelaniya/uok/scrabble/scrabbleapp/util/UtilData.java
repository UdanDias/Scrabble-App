package lk.kelaniya.uok.scrabble.scrabbleapp.util;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

public class UtilData {
    public static String generatePlayerId(){
        return "P/-"+UUID.randomUUID();
    }
    public static String generateGameId(){
        return "G/-"+UUID.randomUUID();
    }

    public static int calcAge(LocalDate dob){
        if(dob == null){
            throw new IllegalArgumentException("Date of Birth not found");
        }

        return Period.between(dob, LocalDate.now()).getYears();
    }

    public static LocalDate generateTodayDate(){
        return LocalDate.now();
    }

    public static String generateUserId(){
        return "U/-"+UUID.randomUUID().toString();
    }
}
