package com.jobtracker.backendJobTracker.auth;



import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.jobtracker.backendJobTracker.user.User;




public record CustomUserDetails(User user) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
        return List.of(authority);
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    /**
     * Spring Security очікує non-null username. Гість не має email, тому віддаємо
     * той самий ідентифікатор, що лежить у JWT subject.
     */
    @Override
    public String getUsername() {
        return user.getEmail() != null
                ? user.getEmail()
                : JwtService.GUEST_SUBJECT_PREFIX + user.getId();
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }

    
}
