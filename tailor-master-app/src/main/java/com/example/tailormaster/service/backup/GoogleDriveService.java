package com.example.tailormaster.service.backup;

import com.example.tailormaster.entity.User;

import java.io.File;

public interface GoogleDriveService {

    String getAuthorizationUrl(User user);

    void handleCallback(User user, String code);

    boolean isConnected(User user);

    void disconnect(User user);

    void uploadBackup(User user, File backupFile);
}