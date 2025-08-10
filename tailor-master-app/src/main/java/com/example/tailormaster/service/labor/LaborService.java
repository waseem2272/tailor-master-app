package com.example.tailormaster.service.labor;

import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.labor.Labor;
import com.example.tailormaster.repository.labor.LaborRepository;
import com.example.tailormaster.util.AuthenticatedUserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class LaborService {

    private static final Logger logger = LogManager.getLogger(LaborService.class);

    private final LaborRepository laborRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public Labor saveLabor(Labor labor) {
        logger.info("Creating Labor: {}", labor.getName());
        labor.setUser(authenticatedUserService.getCurrentUser());
        Labor savedLabor = laborRepository.save(labor);
        logger.debug("Labor created with ID: {}", savedLabor.getId());
        return laborRepository.save(labor);
    }

    public Labor updateLabor(Labor labor) {
        logger.info("Updating Labor: {}", labor.getName());

        // Fetch the existing labor from DB
        Labor existingLabor = laborRepository.findById(labor.getId())
                .orElseThrow(() -> new RuntimeException("Labor not found with ID: " + labor.getId()));

        Labor populateLabor = populateLabor(labor, existingLabor);
        populateLabor.setUser(existingLabor.getUser());
        Labor savedLabor = laborRepository.save(populateLabor);
        logger.info("Labor updated with ID: {}", savedLabor.getId());
        return savedLabor;
    }

    private Labor populateLabor(Labor labor, Labor existingLabor) {
        existingLabor.setName(labor.getName());
        existingLabor.setContact(labor.getContact());
        existingLabor.setSkills(labor.getSkills());
        existingLabor.setJoinDate(labor.getJoinDate());
        existingLabor.setStatus(labor.getStatus());
        return existingLabor;
    }

    public List<Labor> getAllLabors() {
        return laborRepository.findAllByUser(authenticatedUserService.getCurrentUser());
    }

    public Optional<Labor> getLaborById(Long id) {
        return laborRepository.findByIdAndUser(id, authenticatedUserService.getCurrentUser());
    }

    public void deleteLabor(Long id) {
        laborRepository.deleteByIdAndUser(id, authenticatedUserService.getCurrentUser());
    }
}
