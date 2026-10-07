package com.example.tailormaster.controller.backup;

import com.example.tailormaster.entity.BackupHistory;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.entity.UserBackupSettings;
import com.example.tailormaster.enums.BackupStatus;
import com.example.tailormaster.repository.backup.BackupHistoryRepository;
import com.example.tailormaster.service.backup.BackupService;
import com.example.tailormaster.service.backup.UserBackupSettingsService;
import com.example.tailormaster.util.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/backup")
@RequiredArgsConstructor
public class BackupController {

    private final UserBackupSettingsService userBackupSettingsService;
    private final AuthenticatedUserService authenticatedUserService;
    private final BackupService backupService;
    private final BackupHistoryRepository backupHistoryRepository;

    @GetMapping("/settings")
    public String settings(Model model) {
        try {
            User user = authenticatedUserService.getCurrentUser();
            UserBackupSettings settings = userBackupSettingsService.getByUser(user);
            model.addAttribute("settings", settings);
            model.addAttribute("activePage", "backup/settings");
            return "backup/settings";
        } catch (Exception e) {
            log.error("Error loading backup settings", e);
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/settings")
    public String saveSettings(@ModelAttribute("settings") UserBackupSettings settings,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = authenticatedUserService.getCurrentUser();
            UserBackupSettings existingSettings = userBackupSettingsService.getByUser(user);

            existingSettings.setAutomaticBackupEnabled(settings.isAutomaticBackupEnabled());
            existingSettings.setBackupTime(settings.getBackupTime());
            existingSettings.setLocalBackupEnabled(settings.isLocalBackupEnabled());
            existingSettings.setLocalBackupPath(settings.getLocalBackupPath());
            existingSettings.setGoogleDriveEnabled(settings.isGoogleDriveEnabled());
            existingSettings.setRetentionDays(settings.getRetentionDays());

            userBackupSettingsService.save(existingSettings);

            log.info("Backup settings updated successfully for user: {}", user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Backup settings saved successfully.");
        } catch (Exception e) {
            log.error("Error saving backup settings", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to save backup settings.");
        }
        return "redirect:/backup/settings";
    }

    @PostMapping("/now")
    public String backupNow(RedirectAttributes redirectAttributes) {
        try {
            User user = authenticatedUserService.getCurrentUser();
            BackupHistory history = backupService.createLocalBackup(user);

            if (history.getStatus() == BackupStatus.SUCCESS) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Database backup completed successfully."
                );
            } else {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        history.getErrorMessage() != null
                                ? history.getErrorMessage()
                                : "Unable to create database backup."
                );
            }
        } catch (Exception e) {
            log.error("Error starting manual backup", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to create database backup."
            );
        }
        return "redirect:/backup/settings";
    }

    @GetMapping("/history")
    public String history(Model model) {
        try {
            User user = authenticatedUserService.getCurrentUser();
            List<BackupHistory> history = backupHistoryRepository.findByUserOrderByStartedAtDesc(user);
            model.addAttribute("history", history);
            model.addAttribute("activePage", "backup/history");
            return "backup/history";
        } catch (Exception e) {
            log.error("Error loading backup history", e);
            return "redirect:/backup/settings";
        }
    }
}