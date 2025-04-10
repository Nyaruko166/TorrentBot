package me.nyaruko166.torrentskibidi.discord.Listener;

import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.libtorrent4j.AlertListener;
import org.libtorrent4j.SessionManager;
import org.libtorrent4j.TorrentHandle;
import org.libtorrent4j.TorrentInfo;
import org.libtorrent4j.TorrentStatus;
import org.libtorrent4j.alerts.Alert;
import org.libtorrent4j.alerts.AlertType;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class TestListener extends ListenerAdapter {

    private final SessionManager session = new SessionManager();
    private final ConcurrentHashMap<String, TorrentHandle> activeTorrents = new ConcurrentHashMap<>();
    private static final String DOWNLOAD_PATH = "./temp/";

    public TestListener() {
        session.start();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();

        if (message.startsWith("!torrent") && !event.getMessage().getAttachments().isEmpty()) {
            Message.Attachment attachment = event.getMessage().getAttachments().get(0);
            if (attachment.getFileName().endsWith(".torrent")) {
                event.getChannel().sendMessage("Downloading torrent file...").queue();
                File torrentFile = downloadTorrentFileAttachment(attachment, event);
                startTorrentDownload(torrentFile, event);
            } else {
                event.getChannel().sendMessage("Please attach a valid .torrent file!").queue();
            }
        } else if (message.equalsIgnoreCase("!stop torrent")) {
            stopTorrent(event);
            stopTorrent(event);
        }
    }

    @SneakyThrows
    private File downloadTorrentFileAttachment(Message.Attachment attachment, MessageReceivedEvent event) {
        File torrentFile =
                new File(DOWNLOAD_PATH + attachment.getFileName().replace(".torrent", ""),
                        attachment.getFileName());
        if (!torrentFile.exists()) {
            torrentFile.getParentFile().mkdirs();
        }
        //Todo cuwts ddeos chayj
        // Download the .torrent file
        attachment.getProxy().downloadToFile(torrentFile).thenRun(() -> {
            event.getChannel().sendMessage("Saving torrent file: " + attachment.getFileName()).queue();
        }).exceptionally(ex -> {
            event.getChannel().sendMessage("Failed to save torrent file!").queue();
            ex.printStackTrace();
            return null;
        });
        Thread.sleep(5000);
        return torrentFile.getAbsoluteFile();
    }

    private void startTorrentDownload(File torrentFile, MessageReceivedEvent event) {
        TorrentInfo torrentInfo = new TorrentInfo(torrentFile.getAbsoluteFile());
        TorrentHandle handle = session.find(torrentInfo.infoHash());

        if (handle == null) {
            session.download(torrentInfo, torrentFile.getParentFile());
        }

        activeTorrents.put(torrentFile.getName(), handle);
        event.getChannel().sendMessage("Downloading: " + torrentFile.getName()).queue();

        final CountDownLatch signal = new CountDownLatch(1);
        session.addListener(new AlertListener() {
            private int lastProgress = -1;

            @Override
            public int[] types() {
                return null;
            }

            @Override
            public void alert(Alert<?> alert) {
                AlertType type = alert.type();

                switch (type) {
                    case ADD_TORRENT:
                        System.out.println("[INFO] Torrent added successfully.");
                        break;
                    case BLOCK_FINISHED:
                        TorrentStatus status = handle.status();
                        int progress = (int) (status.progress() * 100);
                        long downloadedBytes = status.totalDone();
                        long totalSize = status.totalWanted();
                        int seeds = status.numSeeds();
                        int downloadRate = status.downloadRate();

                        if (seeds == 0) {
                            System.err.println("[ERROR] No seeds available. Aborting.");
                            event.getChannel().sendMessage("No seeds found. Stopping download.").queue();
                            stopAndCleanupTorrent(torrentFile);
                            return;
                        }

                        long remainingBytes = totalSize - downloadedBytes;
                        String etaString = "N/A";
                        if (downloadRate > 0) {
                            long etaSeconds = remainingBytes / downloadRate;
                            etaString = String.format("%02d:%02d:%02d", etaSeconds / 3600, (etaSeconds % 3600) / 60, etaSeconds % 60);
                        }

                        if (progress != lastProgress) {
                            String progressMessage = String.format("[PROGRESS] %s: %d%% (%d/%d bytes) | Seeds: %d | ETA: %s", torrentFile.getName(), progress, downloadedBytes, totalSize, seeds, etaString);
                            System.out.println(progressMessage);
                            if (progress % 10 == 0) {
                                event.getChannel().sendMessage(progressMessage).queue();
                            }
                            lastProgress = progress;
                        }
                        break;
                    case TORRENT_FINISHED:
                        System.out.println("[SUCCESS] Download completed: " + torrentFile.getName());
                        event.getChannel().sendMessage("Download completed: " + torrentFile.getName()).queue();
                        signal.countDown();
                        break;
                }
            }
        });

        Thread seedCheckThread = new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait a few seconds to check seeds
                int seeders = handle.status().numSeeds();
                if (seeders == 0) {
                    System.err.println("[ERROR] No seeds found. Aborting.");
                    event.getChannel().sendMessage("No seeds found. Aborting download.").queue();
                    stopAndCleanupTorrent(torrentFile);
                } else {
                    event.getChannel().sendMessage("Seeds found: " + seeders).queue();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        seedCheckThread.start();

        try {
            signal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void stopTorrent(MessageReceivedEvent event) {
        if (activeTorrents.isEmpty()) {
            event.getChannel().sendMessage("No active torrents to stop.").queue();
            return;
        }

        for (Map.Entry<String, TorrentHandle> entry : activeTorrents.entrySet()) {
            TorrentHandle handle = entry.getValue();
            if (handle != null) {
                handle.pause();
                handle.flushCache();
                session.remove(handle);
                System.out.println("[INFO] Stopped: " + entry.getKey());
                event.getChannel().sendMessage("Stopped torrent: " + entry.getKey()).queue();
            }
        }
        activeTorrents.clear();
    }

    private void stopAndCleanupTorrent(File torrentFile) {
        TorrentHandle handle = activeTorrents.remove(torrentFile.getName());
        if (handle != null) {
            handle.pause();
            handle.flushCache();
            session.remove(handle);
        }

        // Delete downloaded files
        File downloadFolder = new File(DOWNLOAD_PATH);
        if (downloadFolder.exists() && downloadFolder.isDirectory()) {
            for (File file : downloadFolder.listFiles()) {
                if (file.delete()) {
                    System.out.println("[INFO] Deleted: " + file.getAbsolutePath());
                } else {
                    System.err.println("[WARNING] Failed to delete: " + file.getAbsolutePath());
                }
            }
        }
    }
}
