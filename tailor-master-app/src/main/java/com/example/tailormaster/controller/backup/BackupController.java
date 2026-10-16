package com.example.tailormaster.controller.backup;

import com.example.tailormaster.entity.BackupHistory;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.entity.UserBackupSettings;
import com.example.tailormaster.enums.BackupStatus;
import com.example.tailormaster.repository.backup.BackupHistoryRepository;
import com.example.tailormaster.service.backup.BackupService;
import com.example.tailormaster.service.backup.GoogleDriveService;
import com.example.tailormaster.service.backup.UserBackupSettingsService;
import com.example.tailormaster.util.AuthenticatedUserService;
import com.example.tailormaster.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
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
    private final GoogleDriveService googleDriveService;
    private final MessageUtil messageUtil;

    @GetMapping("/settings")
    public String settings(Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = authenticatedUserService.getCurrentUser();
            UserBackupSettings settings = userBackupSettingsService.getByUser(user);
            model.addAttribute("settings", settings);
            model.addAttribute("activePage", "backup/settings");
            return "backup/settings";
        } catch (Exception e) {
            log.error("Error loading backup settings", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("backup.error.settings.load"));
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
            redirectAttributes.addFlashAttribute("successMessage",
                    messageUtil.get("backup.settings.saved"));
        } catch (Exception e) {
            log.error("Error saving backup settings", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("backup.error.settings.save"));
        }
        return "redirect:/backup/settings";
    }

    @PostMapping("/now")
    public String backupNow(RedirectAttributes redirectAttributes) {
        try {
            User user = authenticatedUserService.getCurrentUser();
            BackupHistory history = backupService.createLocalBackup(user);

            if (history.getStatus() == BackupStatus.SUCCESS) {
                redirectAttributes.addFlashAttribute("successMessage",
                        messageUtil.get("backup.success.completed"));
            } else if (history.getStatus() == BackupStatus.PARTIAL_SUCCESS) {
                redirectAttributes.addFlashAttribute("warningMessage",
                        messageUtil.get("backup.warning.partial"));
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        history.getErrorMessage() != null
                                ? history.getErrorMessage()
                                : messageUtil.get("backup.error.create"));
            }
        } catch (Exception e) {
            log.error("Error starting manual backup", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("backup.error.create"));
        }
        return "redirect:/backup/settings";
    }

    @GetMapping("/history")
    public String history(Model model, RedirectAttributes redirectAttributes) {
        try {
            User user = authenticatedUserService.getCurrentUser();
            List<BackupHistory> history =
                    backupHistoryRepository.findByUserOrderByStartedAtDesc(user);
            model.addAttribute("history", history);
            model.addAttribute("activePage", "backup/history");
            return "backup/history";
        } catch (Exception e) {
            log.error("Error loading backup history", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("backup.error.history.load"));
            return "redirect:/backup/settings";
        }
    }

    @GetMapping("/google/connect")
    public String connectGoogleDrive(RedirectAttributes redirectAttributes) {
        try {
            User user = authenticatedUserService.getCurrentUser();
            String authorizationUrl = googleDriveService.getAuthorizationUrl(user);
            return "redirect:" + authorizationUrl;
        } catch (Exception e) {
            log.error("Error connecting Google Drive", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("backup.error.google.connect"));
            return "redirect:/backup/settings";
        }
    }

    @GetMapping("/google/callback")
    public String googleCallback(@RequestParam("code") String code,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = authenticatedUserService.getCurrentUser();
            googleDriveService.handleCallback(user, code);

            redirectAttributes.addFlashAttribute("successMessage",
                    messageUtil.get("backup.google.connected"));
        } catch (Exception e) {
            log.error("Error handling Google Drive callback", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("backup.error.google.connect"));
        }
        return "redirect:/backup/settings";
    }

    @PostMapping("/google/disconnect")
    public String disconnectGoogleDrive(RedirectAttributes redirectAttributes) {
        try {
            User user = authenticatedUserService.getCurrentUser();
            googleDriveService.disconnect(user);

            redirectAttributes.addFlashAttribute("successMessage",
                    messageUtil.get("backup.google.disconnected"));
        } catch (Exception e) {
            log.error("Error disconnecting Google Drive", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("backup.error.google.disconnect"));
        }
        return "redirect:/backup/settings";
    }
}