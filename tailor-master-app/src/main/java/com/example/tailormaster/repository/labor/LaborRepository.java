package com.example.tailormaster.repository.labor;

import com.example.tailormaster.entity.User;
import com.example.tailormaster.entity.labor.Labor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LaborRepository extends JpaRepository<Labor, Long> {
    Optional<Labor> findByName(String name);

    List<Labor> findAllByUser(@Param("currentUser") User currentUser);

    Optional<Labor> findByIdAndUser(Long id, User currentUser);

    void deleteByIdAndUser(Long id, User currentUser);
}
