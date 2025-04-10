package me.nyaruko166.torrentskibidi.drive;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.IOException;

public class FileUploadProgressListener implements MediaHttpUploaderProgressListener {

    private OnProgressListener onProgressListener;

    public FileUploadProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

    @Override
    public void progressChanged(MediaHttpUploader uploader) throws IOException {
        switch (uploader.getUploadState()) {
            case INITIATION_STARTED:
                onProgressListener.onInitStarted();
                break;
            case INITIATION_COMPLETE:
                onProgressListener.onInitCompleted();
                break;
            case MEDIA_IN_PROGRESS:
                onProgressListener.onMediaProgress(uploader.getProgress() * 100);
                break;
            case MEDIA_COMPLETE:
                onProgressListener.onMediaCompleted();
                break;
        }
    }

}
