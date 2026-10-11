package com.jobtracker.backendJobTracker.auth;

import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    /**
     * @param principal email зареєстрованого юзера або {@code guest:<uuid>} для
     *                  гостьового акаунта (див. {@link JwtService#GUEST_SUBJECT_PREFIX}).
     */
    @Override
    public UserDetails loadUserByUsername(String principal) throws UsernameNotFoundException {
        if (principal != null && principal.startsWith(JwtService.GUEST_SUBJECT_PREFIX)) {
            return loadGuest(principal.substring(JwtService.GUEST_SUBJECT_PREFIX.length()));
        }
        User user = userRepository.findByEmail(principal)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + principal));
        return new CustomUserDetails(user);
    }

    private UserDetails loadGuest(String rawId) {
        UUID id;
        try {
            id = UUID.fromString(rawId);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException("Malformed guest subject: " + rawId);
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Guest not found: " + rawId));
        return new CustomUserDetails(user);
    }

}
