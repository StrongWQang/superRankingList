package com.example.superrankinglist.controller;

import com.example.superrankinglist.pojo.User;
import com.example.superrankinglist.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        return ResponseEntity.ok(userService.updateUser(user));
    }

    @PutMapping("/{id}/score")
    public ResponseEntity<User> updateUserScore(@PathVariable Long id, @RequestParam Double score) {
        return ResponseEntity.ok(userService.updateUserScore(id, score));
    }

    @GetMapping("/top")
    public ResponseEntity<List<User>> getTopUsers(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(userService.getTopUsers(limit));
    }

    @GetMapping("/score-range")
    public ResponseEntity<List<User>> getUsersByScoreRange(
            @RequestParam Double minScore,
            @RequestParam Double maxScore) {
        return ResponseEntity.ok(userService.getUsersByScoreRange(minScore, maxScore));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}