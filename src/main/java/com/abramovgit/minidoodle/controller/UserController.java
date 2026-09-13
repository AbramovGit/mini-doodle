package com.abramovgit.minidoodle.controller;

import com.abramovgit.minidoodle.api.UserCreateRequest;
import com.abramovgit.minidoodle.api.UserResponse;
import com.abramovgit.minidoodle.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request,
                                               UriComponentsBuilder uriBuilder) {
        UserResponse response = userService.create(request);
        URI location = uriBuilder.path("/api/users/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{userId}")
    public UserResponse get(@PathVariable Long userId) {
        return userService.get(userId);
    }
}
