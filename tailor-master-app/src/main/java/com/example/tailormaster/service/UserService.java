package com.example.tailormaster.service;

import com.example.tailormaster.entity.Role;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.repository.RoleRepository;
import com.example.tailormaster.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Role> getRoles() {
        return roleRepository.findAll();
    }

    public void registerUser(User user, String selectedRole) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);

        Set<Role> roles = new HashSet<>();

        // Check if a role is selected from the dropdown
        if (selectedRole != null && !selectedRole.isEmpty()) {
            Role role = roleRepository.findByName(selectedRole);
            roles.add(role);
        } else {
            // Assign default role (e.g., ROLE_STAFF) if no role is selected
            roles.add(roleRepository.findByName(selectedRole));
        }

        user.setRoles(roles);
        userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return this.userRepository.findAll();
    }

    public Optional<User> findByUsername(String name) {
        return this.userRepository.findByUsername(name);
    }

    // Get the currently logged-in user
    public User getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    // Update user profile
    @Transactional
    public void updateUser(User updatedUser) {
        User existingUser = getLoggedInUser();

        // Updating allowed fields only
        existingUser.setFullName(updatedUser.getFullName());
        existingUser.setFathersName(updatedUser.getFathersName());
        existingUser.setPhone1(updatedUser.getPhone1());
        existingUser.setPhone2(updatedUser.getPhone2());
        existingUser.setDateOfBirth(updatedUser.getDateOfBirth());
        existingUser.setShopName(updatedUser.getShopName());
        existingUser.setProprietorName(updatedUser.getProprietorName());
        existingUser.setShopAddress(updatedUser.getShopAddress());
        existingUser.setShortCode(updatedUser.getShortCode());

        userRepository.save(existingUser);
    }
}
