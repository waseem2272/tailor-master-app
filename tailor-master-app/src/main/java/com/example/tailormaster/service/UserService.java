package com.example.tailormaster.service;

import com.example.tailormaster.entity.Role;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.repository.RoleRepository;
import com.example.tailormaster.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
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
}
