package com.example.tailormaster.repository.backup;

import com.example.tailormaster.entity.User;
import com.example.tailormaster.entity.UserBackupSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBackupSettingsRepository extends JpaRepository<UserBackupSettings, Long> {

    Optional<UserBackupSettings> findByUser(User user);
    List<UserBackupSettings> findByAutomaticBackupEnabledTrue();
}