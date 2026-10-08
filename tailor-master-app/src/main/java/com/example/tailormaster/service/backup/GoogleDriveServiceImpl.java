package com.example.tailormaster.service.backup;

import com.example.tailormaster.entity.User;
import com.example.tailormaster.entity.UserBackupSettings;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.api.services.drive.Drive;
import com.google.api.client.http.FileContent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDriveServiceImpl implements GoogleDriveService {

    private static final String REDIRECT_URI =
            "http://localhost:8080/tailor-master/backup/google/callback";

    private static final JsonFactory JSON_FACTORY =
            GsonFactory.getDefaultInstance();

    private static final List<String> SCOPES =
            Collections.singletonList(DriveScopes.DRIVE_FILE);

    @Value("${google.drive.client-secret-file}")
    private String clientSecretFile;

    @Value("${google.drive.tokens-directory}")
    private String tokensDirectory;

    private final UserBackupSettingsService userBackupSettingsService;

    @Override
    public String getAuthorizationUrl(User user) {
        try {
            GoogleAuthorizationCodeFlow flow = createFlow();

            return flow.newAuthorizationUrl()
                    .setRedirectUri(REDIRECT_URI)
                    .setAccessType("offline")
                    .setApprovalPrompt("force")
                    .build();

        } catch (Exception e) {
            log.error("Error generating Google Drive authorization URL for user: {}",
                    user.getUsername(), e);
            throw new RuntimeException("Unable to connect Google Drive.", e);
        }
    }

    @Override
    public void handleCallback(User user, String code) {
        try {
            GoogleAuthorizationCodeFlow flow = createFlow();

            GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri(REDIRECT_URI)
                    .execute();

            Credential credential = flow.createAndStoreCredential(
                    tokenResponse,
                    user.getId().toString()
            );

            UserBackupSettings settings =
                    userBackupSettingsService.getByUser(user);

            if (settings != null) {
                settings.setGoogleDriveConnected(true);
                userBackupSettingsService.save(settings);
            }

            log.info("Google Drive connected successfully for user: {}",
                    user.getUsername());

        } catch (Exception e) {
            log.error("Error handling Google Drive callback for user: {}",
                    user.getUsername(), e);
            throw new RuntimeException("Unable to connect Google Drive.", e);
        }
    }

    @Override
    public boolean isConnected(User user) {
        try {
            UserBackupSettings settings =
                    userBackupSettingsService.getByUser(user);

            return settings != null && settings.isGoogleDriveConnected();

        } catch (Exception e) {
            log.error("Error checking Google Drive connection for user: {}",
                    user.getUsername(), e);
            return false;
        }
    }

    @Override
    public void disconnect(User user) {
        try {
            UserBackupSettings settings =
                    userBackupSettingsService.getByUser(user);

            if (settings != null) {
                settings.setGoogleDriveConnected(false);
                settings.setGoogleDriveFolderId(null);
                userBackupSettingsService.save(settings);
            }

            log.info("Google Drive disconnected for user: {}",
                    user.getUsername());

        } catch (Exception e) {
            log.error("Error disconnecting Google Drive for user: {}",
                    user.getUsername(), e);
            throw new RuntimeException("Unable to disconnect Google Drive.", e);
        }
    }

    private GoogleAuthorizationCodeFlow createFlow() throws Exception {
        GoogleClientSecrets clientSecrets;

        try (InputStream inputStream =
                     new FileInputStream(clientSecretFile)) {

            clientSecrets = GoogleClientSecrets.load(
                    JSON_FACTORY,
                    new InputStreamReader(
                            inputStream,
                            StandardCharsets.UTF_8
                    )
            );
        }

        File tokenDirectory = new File(tokensDirectory);

        if (!tokenDirectory.exists() && !tokenDirectory.mkdirs()) {
            throw new IOException(
                    "Unable to create Google Drive token directory: "
                            + tokensDirectory
            );
        }

        FileDataStoreFactory dataStoreFactory =
                new FileDataStoreFactory(tokenDirectory);

        return new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                clientSecrets,
                SCOPES
        )
                .setDataStoreFactory(dataStoreFactory)
                .setAccessType("offline")
                .build();
    }

    @Override
    public void uploadBackup(User user, File backupFile) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User is required.");
            }

            if (backupFile == null || !backupFile.exists()) {
                throw new IllegalArgumentException("Backup file does not exist.");
            }

            GoogleAuthorizationCodeFlow flow = createFlow();

            Credential credential = flow.loadCredential(
                    user.getId().toString()
            );

            if (credential == null) {
                throw new IllegalStateException(
                        "Google Drive is not connected for user: " + user.getUsername()
                );
            }

            Drive driveService = new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JSON_FACTORY,
                    credential
            )
                    .setApplicationName("TailorMaster")
                    .build();

            com.google.api.services.drive.model.File fileMetadata =
                    new com.google.api.services.drive.model.File();

            String folderId = getOrCreateBackupFolder(user, driveService);

            fileMetadata.setName(backupFile.getName());
            fileMetadata.setParents(Collections.singletonList(folderId));

            FileContent mediaContent = new FileContent(
                    "application/sql",
                    backupFile
            );

            com.google.api.services.drive.model.File uploadedFile =
                    driveService.files()
                            .create(fileMetadata, mediaContent)
                            .setFields("id, name, size")
                            .execute();

            log.info(
                    "Backup uploaded to Google Drive successfully for user: {}, file: {}, fileId: {}",
                    user.getUsername(),
                    uploadedFile.getName(),
                    uploadedFile.getId()
            );

        } catch (Exception e) {
            log.error(
                    "Error uploading backup to Google Drive for user: {}",
                    user != null ? user.getUsername() : "unknown",
                    e
            );
            throw new RuntimeException(
                    "Unable to upload backup to Google Drive.",
                    e
            );
        }
    }

    private String getOrCreateBackupFolder(User user, Drive driveService) {
        try {
            UserBackupSettings settings =
                    userBackupSettingsService.getByUser(user);

            if (settings.getGoogleDriveFolderId() != null
                    && !settings.getGoogleDriveFolderId().isBlank()) {
                return settings.getGoogleDriveFolderId();
            }

            String query = "name = 'TailorMaster Backups'"
                    + " and mimeType = 'application/vnd.google-apps.folder'"
                    + " and trashed = false";

            FileList result = driveService.files()
                    .list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute();

            String folderId;

            if (result.getFiles() != null && !result.getFiles().isEmpty()) {
                folderId = result.getFiles().get(0).getId();

                log.info(
                        "Existing Google Drive backup folder found for user: {}, folderId: {}",
                        user.getUsername(),
                        folderId
                );
            } else {
                com.google.api.services.drive.model.File folderMetadata =
                        new com.google.api.services.drive.model.File();

                folderMetadata.setName("TailorMaster Backups");
                folderMetadata.setMimeType("application/vnd.google-apps.folder");

                com.google.api.services.drive.model.File folder = driveService.files()
                        .create(folderMetadata)
                        .setFields("id, name")
                        .execute();

                folderId = folder.getId();

                log.info(
                        "Google Drive backup folder created for user: {}, folderId: {}",
                        user.getUsername(),
                        folderId
                );
            }

            settings.setGoogleDriveFolderId(folderId);
            userBackupSettingsService.save(settings);

            return folderId;

        } catch (Exception e) {
            log.error(
                    "Error getting or creating Google Drive backup folder for user: {}",
                    user.getUsername(),
                    e
            );
            throw new RuntimeException(
                    "Unable to create Google Drive backup folder.",
                    e
            );
        }
    }
}