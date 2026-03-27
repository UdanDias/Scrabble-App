package lk.kelaniya.uok.scrabble.scrabbleapp.entity.security;

import jakarta.persistence.*;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "admin_request")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminRequestEntity implements Serializable {
    @Id
    private String requestId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    private RequestStatus status; // PENDING, APPROVED, REJECTED

    private LocalDate requestedDate;
    private LocalDate resolvedDate;
}