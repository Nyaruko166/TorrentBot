package me.nyaruko166.torrentskibidi.util;

import lombok.SneakyThrows;
import me.nyaruko166.torrentskibidi.discord.NyaUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.libtorrent4j.AlertListener;
import org.libtorrent4j.SessionManager;
import org.libtorrent4j.TorrentHandle;
import org.libtorrent4j.TorrentInfo;
import org.libtorrent4j.TorrentStatus;
import org.libtorrent4j.alerts.AddTorrentAlert;
import org.libtorrent4j.alerts.Alert;
import org.libtorrent4j.alerts.AlertType;
import org.libtorrent4j.alerts.BlockFinishedAlert;
import org.libtorrent4j.alerts.TorrentFinishedAlert;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class TorrentHelper {

    private static final Logger log = LogManager.getLogger(TorrentHelper.class);

    @SneakyThrows
    public static File torrentDownload(SessionManager session, File torrentFile, MessageReceivedEvent event, String notificationId) {
        final int[] lastProgress = {-1};

        final boolean[] check = {false};

        File downloadFolder = torrentFile.getParentFile();
//        System.out.println("Using libtorrent version: " + LibTorrent.version());

//        final SessionManager session = new SessionManager();
        final CountDownLatch signal = new CountDownLatch(1);

        session.addListener(new AlertListener() {
            @Override
            public int[] types() {
                return null;
            }

            @Override
            public void alert(Alert<?> alert) {
                AlertType type = alert.type();
                switch (type) {
                    case ADD_TORRENT:
//                        System.out.println("[INFO] Torrent added successfully.");
                        ((AddTorrentAlert) alert).handle().resume();
                        break;
                    case BLOCK_FINISHED:
                        BlockFinishedAlert blockAlert = (BlockFinishedAlert) alert;
                        TorrentHandle handle = blockAlert.handle();
                        TorrentStatus status = handle.status();

                        int progress = (int) (status.progress() * 100);
                        long downloadedBytes = session.stats().totalDownload();
                        long totalSize = status.totalWanted();
                        int seeds = status.numSeeds();
                        int downloadRate = status.downloadRate(); // Bytes per second

                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setFooter(NyaUtil.getTimeStamp(event));

                        if (seeds == 0) {
                            eb.setColor(Color.RED);
                            eb.setTitle("Error");
                            eb.setDescription("No seeds available during download. Aborting.");
                            event.getChannel().editMessageEmbedsById(notificationId, eb.build()).queue();
                            System.err.println("[ERROR] No seeds available during download. Aborting.");
                            cleanupFiles(torrentFile.getName());
                            session.stop();
                            return;
                        }

                        long remainingBytes = totalSize - downloadedBytes;
                        String etaString;
                        if (downloadRate > 0) {
                            long etaSeconds = remainingBytes / downloadRate;
                            etaString = String.format("%02d:%02d:%02d", etaSeconds / 3600, (etaSeconds % 3600) / 60, etaSeconds % 60);
                        } else {
                            etaString = "N/A";
                        }

                        if (progress != lastProgress[0]) { // Print only when progress changes
                            eb.setColor(Color.CYAN);
                            eb.setTitle("Downloading Progress");
                            eb.setDescription(blockAlert.torrentName());
                            eb.addField("Progress", progress + "%", true);
                            eb.addField("Downloaded Bytes", "%d/%d bytes".formatted(downloadedBytes, totalSize), true);
                            eb.addField("ETA", etaString, true);
//                            String res = "[PROGRESS] %s: %d%% (%d/%d bytes) | Seeds: %d | ETA: %s\n"
//                                    .formatted(blockAlert.torrentName(), progress, downloadedBytes, totalSize, seeds, etaString);
                            if (progress % 10 == 0) {
                                eb.setFooter(NyaUtil.getTimeStamp(event));
                                event.getChannel().editMessageEmbedsById(notificationId, eb.build()).queue();
//                                log.info(res);
                            }
                            lastProgress[0] = progress;
                        }
                        break;
                    case TORRENT_FINISHED:
                        event.getChannel().editMessageEmbedsById(notificationId, new EmbedBuilder()
                                .setColor(Color.GREEN)
                                .setTitle("Torrent Download Completed !!")
                                .setDescription("Download %s completed!%n".formatted(((TorrentFinishedAlert) alert).torrentName()))
                                .setFooter(NyaUtil.getTimeStamp(event))
                                .build()).queue();

                        String res = "[SUCCESS] Download %s completed!%n".formatted(((TorrentFinishedAlert) alert).torrentName());
                        log.info(res);
                        signal.countDown();
                        check[0] = true;
                        break;
                }
            }
        });

        session.start();
        TorrentInfo torrentInfo = new TorrentInfo(torrentFile);

        session.download(torrentInfo, downloadFolder);

        signal.await();
        session.stop();

        if (check[0]) {
            return downloadFolder;
        } else {
            return null;
        }
    }

    public static boolean cleanupFiles(String fileName) {
        boolean check = false;
        File downloadFolder = new File("./temp");
        if (downloadFolder.exists() && downloadFolder.isDirectory()) {
            File[] files = downloadFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().equals(fileName.replace(".torrent", ""))) {
                        try {
                            FileUtils.deleteDirectory(file);
                            if (!file.exists()) check = true;
                        } catch (IOException e) {
                            log.error(e);
                        }
                        if (check) {
                            log.info("[INFO] Deleted: {}", file.getAbsolutePath());
                        } else {
                            log.error("[WARNING] Failed to delete: {}", file.getAbsolutePath());
                        }
                    }
                }
            }
        }
        return check;
    }

    public static boolean cleanupFolder() {
        boolean check = false;
        File downloadFolder = new File("./temp");
        if (downloadFolder.exists() && downloadFolder.isDirectory()) {
            File[] files = downloadFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    try {
                        FileUtils.delete(file);
                    } catch (IOException e) {
                        log.error("Error when cleaning up folder.", e);
                    }
                }
            }
        }
        return check;
    }

}
