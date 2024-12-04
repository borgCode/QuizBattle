package org.borg.backend.player;

import jakarta.persistence.*;
import lombok.*;
import org.borg.backend.multiplayer.MultiplayerSession;
import org.borg.backend.role.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "players")
public class Player implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;
    
    @Column(unique = true)
    private String username;
    private String password;
    private String displayName;
    private int numOfGames;
    private int numOfWins;
    private int numOfLosses;
    private boolean accountLocked;
    private boolean enabled;
    @ManyToMany(mappedBy = "players")
    private List<MultiplayerSession> session;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Role> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles
                .stream()
                .map(r -> new SimpleGrantedAuthority(r.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
