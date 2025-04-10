package me.nyaruko166.torrentskibidi.drive;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.log4j.Log4j2;
import me.nyaruko166.torrentskibidi.discord.NyaUtil;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Log4j2
public class GoogleDriveService {

    private static final int THRESHOLD = 1024;
    private static final String[] UNITS = new String[]{"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
    private final Drive driveService = getService();
    private final String SERVICE_ACCOUNT_KEY_PATH = "./libs/cred.json";
    private final String ROOT_FOLDER_ID = "1Oc4XkS0PW0GSaK2nHAe1dmW9yUwPk2cY";

    private Drive getService() {
        // Create the credentials object
        GoogleCredentials credentials = null;
        try {
            // Load the service account key JSON file
            FileInputStream serviceAccountStream = new FileInputStream(SERVICE_ACCOUNT_KEY_PATH);
            credentials = GoogleCredentials
                    .fromStream(serviceAccountStream)
                    .createScoped(Collections.singleton(DriveScopes.DRIVE_FILE));
        } catch (IOException e) {
            log.error("cred.json for service account not found. Put it in ./libs", e);
            System.exit(1);
        }

        // Build the Drive service object
        assert credentials != null;
        return new Drive.Builder(
                new NetHttpTransport(),
                new GsonFactory(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Google-Drive-Resumable-Uploader")
                .build();
    }

    public List<File> getAllFolder() {
        String query = "mimeType = '%s' and trashed = false".formatted(MimeType.GOOGLE_APPS_FOLDER);
        FileList result = null;
        try {
            result = driveService.files().list()
                                 .setSpaces("drive")
                                 .setQ(query)
                                 .setFields("files(id, name, size)")
                                 .execute();
        } catch (IOException e) {
            log.error(e);
        }

        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            log.info("No folder found.");
            return Collections.emptyList();
        }
        return files;
    }

    public String createFolder(String folderName) {
        File fileMetadata = new File();
        fileMetadata.setParents(Collections.singletonList(ROOT_FOLDER_ID));
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType(MimeType.GOOGLE_APPS_FOLDER);

        try {
            File folder = driveService.files().create(fileMetadata).execute();
            log.info("Folder created: {} | Id: {}", folder.getName(), folder.getId());
            return folder.getId();
        } catch (IOException e) {
            log.error("Error when creating new folder.");
            log.error(e);
            return null;
        }
    }

    public List<File> getAllTrash() {
        String query = "trashed = true";
        FileList result = null;
        try {
            result = driveService.files().list()
                                 .setQ(query) // Query to list all files
                                 .setSpaces("drive")
                                 .setFields("files(id, name, size)")
                                 .execute();
        } catch (IOException e) {
            log.error(e);
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            log.info("No files found.");
            return Collections.emptyList();
        }
        return files;
    }

    public List<File> getAllFile() {
        // List all files uploaded by the service account
        String query = "mimeType != 'application/vnd.google-apps.folder' and trashed = false";
        FileList result = null;
        try {
            result = driveService.files().list()
                                 .setQ(query) // Query to list all files
                                 .setSpaces("drive")
                                 .setFields("files(id, name, size)")
                                 .execute();
        } catch (IOException e) {
            log.error(e);
        }

        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            log.info("No files found.");
            return Collections.emptyList();
        }
        return files;
    }

    public String uploadFile(java.io.File file, String mimeType, MessageReceivedEvent event) {
        try {
            // Create file metadata
            File fileMetadata = new File();
            fileMetadata.setName(file.getName());
            fileMetadata.setParents(Collections.singletonList(ROOT_FOLDER_ID)); // Set the parent folder

            // Specify the file content and media type
            InputStreamContent mediaContent = new InputStreamContent(
                    mimeType, new FileInputStream(file));

            // Set file length for large uploads
            mediaContent.setLength(file.length());

            Drive.Files.Create request = driveService.files().create(fileMetadata, mediaContent);
            request.getMediaHttpUploader().setDirectUploadEnabled(false);
            request.getMediaHttpUploader().setChunkSize(MediaHttpUploader.DEFAULT_CHUNK_SIZE);

            // Set a progress listener
            request.getMediaHttpUploader()
                   .setProgressListener(
                           new FileUploadProgressListener(
                           new OnProgressListener() {
                @Override
                public void onInitStarted() {
                    NyaUtil.sendChannelMessage(event,"");
                }

                @Override
                public void onInitCompleted() {

                }

                @Override
                public void onMediaProgress(double progress) {

                }

                @Override
                public void onMediaCompleted() {

                }
            }));

            // Execute the upload
            File uploadedFile = request.execute();

            log.info("File uploaded with ID: {}", uploadedFile.getId());
            log.info("Download link: https://drive.usercontent.google.com/download?id={}", uploadedFile.getId());
            return uploadedFile.getId();
        } catch (IOException e) {
            log.error("Error when uploading file.");
            log.error(e);
            return null;
        }
    }

    public String updateFile(String filedId, File file) {
        try {
            File returnFile = driveService.files().update(filedId, file).execute();
            return returnFile.getId();
        } catch (IOException e) {
            log.error("Error when updating file.");
            log.error(e);
            return null;
        }
    }

    public String moveToTrash(String fileId) {
        try {
            File file = driveService.files().update(fileId, new File().setTrashed(true)).execute();
            return file.getId();
        } catch (IOException e) {
            log.error("Error when trashing file.");
            log.error(e);
            return null;
        }
    }

    public void emptyTrash() {
        try {
            driveService.files().emptyTrash();
        } catch (IOException e) {
            log.error("Error when emptying trash.");
            log.error(e);
        }
    }

    public void deleteFile(String fileId) {
        // Delete the file with the given fileId
        try {
            driveService.files().delete(fileId).execute();
            log.info("File with ID {} has been deleted.", fileId);
        } catch (IOException e) {
            log.error("Error occurred: {}", String.valueOf(e));
        }
    }

    public String format(double size) {
        size = size < 0 ? 0 : size;
        int u;
        for (u = 0; u < UNITS.length - 1 && size >= THRESHOLD; ++u) {
            size /= 1024;
        }
        return String.format("%.0f %s", size, UNITS[u]);
    }

}
