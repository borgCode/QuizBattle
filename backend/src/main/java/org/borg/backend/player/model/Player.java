package org.borg.backend.player.model;

import jakarta.persistence.*;
import lombok.*;
import org.borg.backend.achievement.Achievement;
import org.borg.backend.friendship.model.Friendship;
import org.borg.backend.multiplayer.model.MultiplayerSession;
import org.borg.backend.auth.model.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

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
    @OneToOne(mappedBy = "player", cascade = CascadeType.ALL)
    private Stats stats;
    private boolean accountLocked;
    private boolean enabled;

    private String avatarPath;

    @ManyToMany(mappedBy = "players")
    private List<MultiplayerSession> session;

    @OneToMany(mappedBy = "player1", cascade = CascadeType.ALL)
    private Set<Friendship> friendshipsInitiated = new HashSet<>();

    @OneToMany(mappedBy = "player2", cascade = CascadeType.ALL)
    private Set<Friendship> friendshipsReceived = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Role> roles;
    

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (this.stats == null) {
            this.stats = new Stats();
            this.stats.setPlayer(this);
        }
    }
    

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
