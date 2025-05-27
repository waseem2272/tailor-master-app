package com.example.tailormaster.repository.labor;

import com.example.tailormaster.entity.labor.Labor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LaborRepository extends JpaRepository<Labor, Long> {
    Optional<Labor> findByName(String name);
}
