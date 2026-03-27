package lk.kelaniya.uok.scrabble.scrabbleapp.entity.security;

import jakarta.persistence.*;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.Role;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.PlayerEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "user")
public class UserEntity implements Serializable, UserDetails {
    @Id
    private String userId;
    private String firstName;
    private String lastName;
    @Column(unique = true, nullable = false)
    private String email;
    private String password;
    @Enumerated(EnumType.STRING)
    private Role userRole= Role.USER;
    @OneToOne(optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + userRole.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }
}
