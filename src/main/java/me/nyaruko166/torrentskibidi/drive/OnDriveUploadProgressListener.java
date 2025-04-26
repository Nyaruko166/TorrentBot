package me.nyaruko166.torrentskibidi.drive;

public interface OnDriveUploadProgressListener {
    void onInitStarted();
    void onInitCompleted();
    void onMediaProgress(double progress);
    void onMediaCompleted();
}
