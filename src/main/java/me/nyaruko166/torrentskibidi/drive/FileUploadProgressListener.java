package me.nyaruko166.torrentskibidi.drive;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;

import java.io.IOException;

public class FileUploadProgressListener implements MediaHttpUploaderProgressListener {

    private OnDriveUploadProgressListener onDriveUploadProgressListener;

    public FileUploadProgressListener(OnDriveUploadProgressListener onDriveUploadProgressListener) {
        this.onDriveUploadProgressListener = onDriveUploadProgressListener;
    }

    @Override
    public void progressChanged(MediaHttpUploader uploader) throws IOException {
        switch (uploader.getUploadState()) {
            case INITIATION_STARTED:
                onDriveUploadProgressListener.onInitStarted();
                break;
            case INITIATION_COMPLETE:
                onDriveUploadProgressListener.onInitCompleted();
                break;
            case MEDIA_IN_PROGRESS:
                onDriveUploadProgressListener.onMediaProgress(uploader.getProgress() * 100);
                break;
            case MEDIA_COMPLETE:
                onDriveUploadProgressListener.onMediaCompleted();
                break;
        }
    }

}
