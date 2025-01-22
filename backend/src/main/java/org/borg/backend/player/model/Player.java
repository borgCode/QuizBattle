package org.borg.backend.player.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.borg.backend.auth.model.Role;
import org.borg.backend.game.multiplayer.model.SessionPlayer;
import org.borg.backend.social.friendship.model.Friendship;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "players",
indexes = {
        @Index(name = "IX_player_username", columnList = "username", unique = true)
})
public class Player implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String password;
    private String displayName;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "stats_id", referencedColumnName = "id")
    private Stats stats;
    private boolean accountLocked;
    private boolean enabled;

    private String avatarPath;

    @OneToMany(mappedBy = "player")
    private List<SessionPlayer> sessions;

    @OneToMany(mappedBy = "player1", cascade = CascadeType.ALL)
    private Set<Friendship> friendshipsInitiated = new HashSet<>();

    @OneToMany(mappedBy = "player2", cascade = CascadeType.ALL)
    private Set<Friendship> friendshipsReceived = new HashSet<>();

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
