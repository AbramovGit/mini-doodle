package com.abramovgit.minidoodle.service;

import com.abramovgit.minidoodle.api.UserCreateRequest;
import com.abramovgit.minidoodle.api.UserResponse;
import com.abramovgit.minidoodle.domain.Calendar;
import com.abramovgit.minidoodle.domain.User;
import com.abramovgit.minidoodle.exception.ConflictException;
import com.abramovgit.minidoodle.exception.ResourceNotFoundException;
import com.abramovgit.minidoodle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("A user with this email already exists");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setTimezone(request.timezone());

        Calendar calendar = new Calendar();
        calendar.setUser(user);
        user.setCalendar(calendar);

        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long userId) {
        return userRepository.findById(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getTimezone());
    }
}
