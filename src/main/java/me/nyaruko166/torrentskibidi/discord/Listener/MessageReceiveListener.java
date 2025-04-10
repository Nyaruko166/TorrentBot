package me.nyaruko166.torrentskibidi.discord.Listener;

import me.nyaruko166.torrentskibidi.discord.NyaUtil;
import me.nyaruko166.torrentskibidi.drive.GoogleDriveService;
import me.nyaruko166.torrentskibidi.drive.MimeType;
import me.nyaruko166.torrentskibidi.util.TorrentHelper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.libtorrent4j.SessionManager;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MessageReceiveListener extends ListenerAdapter {

    private final String PREFIX = "!";
    private final String DOWNLOAD_PATH = "./temp/";
    public ConcurrentHashMap<String, SessionManager> activeSessions = new ConcurrentHashMap<>();

    private final Logger log = LogManager.getLogger(MessageReceiveListener.class);
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final GoogleDriveService driveService = new GoogleDriveService();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return; // Who care about bot? Bruh wanna make a recursive bot xD
        String message = event.getMessage().getContentRaw();

        // getContentRaw() is an atomic getter
        // getContentDisplay() is a lazy getter which modifies the content for e.g. console view (strip discord formatting)


        if (message.startsWith(PREFIX)) {
            String[] parts = message.split("\\s+", 2); // Split into command and arguments

            String command = parts[0].substring(PREFIX.length()).toLowerCase(); // Extract command (e.g., "!stop")
            String args = (parts.length > 1) ? parts[1] : ""; // Extract arguments if present
//            String command = message.substring(PREFIX.length()).toLowerCase();

            log.info("Author: {} | Command: {} | Args: {}", event.getAuthor().getName(), command, args);
            switch (command) {
                case "ping" -> event.getChannel().sendMessage("Pong nigga!").queue();
//                case "setup torrent" -> {
//                    if (!hasAdminPermission(event)) return;
//                    String guildID = event.getGuild().getId();
//                    String channelID = event.getChannel().getId();
//                    event.getChannel().sendMessageEmbeds(new EmbedBuilder()
//                                 .setAuthor(event.getAuthor().getName(), "https://www.facebook.com/nyaruko166",
//                                         event.getAuthor().getAvatarUrl())
//                                 .setTitle("Setting torrent channel...")
//                                 .setDescription("Day la description? What do u expect?")
//                                 .setFooter(String.format("%s - %s", event.getJDA().getSelfUser().getName(),
//                                                 LocalDateTime.now().format(timeFormatter)),
//                                         event.getJDA().getSelfUser().getAvatarUrl())
//                                 .setColor(Color.GREEN)
//                                 .build())
//                         .queue();
//                    event.getMessage().delete().queue();
//                    Config.getProperty().setGuildId(guildID);
//                    Config.getProperty().setChannelId(channelID);
//                    Config.updateConfig();
//                }
                case "prune" -> handlePruneCommand(event, args);
                case "torrent" -> startTorrentDownload(event);
                case "stop" -> stopTorrentDownload(event, args);
                case "help" -> sendHelpMessage(event);

                default ->
                        event.getChannel().sendMessage("Unknown command! Use `!help` for a list of commands.").queue();
            }
        }
    }

    private void startTorrentDownload(MessageReceivedEvent event) {
        // Check if the message contains attachments
        List<Message.Attachment> attachments = event.getMessage().getAttachments();

        if (attachments.isEmpty()) {
            event.getChannel().sendMessage("There is no attachment...").queue();
            return;
        }

        for (Message.Attachment attachment : attachments) {
            if (attachment.getFileName().endsWith(".torrent")) {
                event.getChannel().sendMessage("Detected a .torrent file: " + attachment.getFileName()).queue();

                String torrentName = attachment.getFileName().replace(".torrent", "");

                File torrentFile =
                        new File(DOWNLOAD_PATH + torrentName, attachment.getFileName());
                if (!torrentFile.exists()) {
                    torrentFile.getParentFile().mkdirs();
                }

                attachment.getProxy().downloadToFile(torrentFile).thenRun(() -> {
                    event.getChannel().sendMessage("Downloading torrent file: " + attachment.getFileName()).queue();
                    SessionManager session = new SessionManager();
                    activeSessions.put(torrentFile.getName(), session);
                    NyaUtil.sendChannelMessage(event, "To stop download using !stop %s".formatted(torrentFile.getName()));
                    File downloadFolder = TorrentHelper.torrentDownload(session, torrentFile, event);
                    activeSessions.remove(torrentFile.getName());
                    File zipFile = new File(DOWNLOAD_PATH + torrentName + ".zip");
                    if (zipFile.exists()) {
                        try {
                            FileUtils.delete(zipFile); //Clean if zipfile exist
                        } catch (IOException e) {
                            log.error("Error when cleaning Zip file", e);
                        }
                    }
                    String authorTag = event.getAuthor().getAsTag();
                    NyaUtil.sendChannelMessage(event, "Creating archive from torrent folder, may take sometime...");
                    if (NyaUtil.compressFolder(zipFile, authorTag.substring(0, authorTag.indexOf("#")), downloadFolder)) {
                        NyaUtil.sendChannelMessage(event, "Created archive from torrent folder, starting to upload...");
                        String fileId = driveService.uploadFile(zipFile, MimeType.APPLICATION_ZIP, event);
                        if (fileId != null) {
                            NyaUtil.sendChannelMessage(event, "Download link: https://drive.usercontent.google.com/download?id=%s".formatted(fileId));
//                            Todo clean up
//                            TorrentHelper.cleanupFolder();
                        } else NyaUtil.sendChannelMessage(event, "Error when uploading torrent file to drive.");
                    } else
                        NyaUtil.sendChannelMessage(event, "Failed to create archive from torrent...");
                }).exceptionally(ex -> {
                    event.getChannel().sendMessage("Failed to save torrent file!").queue();
                    log.error("Failed to save torrent file!", ex);
                    return null;
                });
                return; // Stop after finding the first .torrent file
            } else {
                event.getChannel().sendMessage("Please attach a valid .torrent file!").queue();
            }
        }
    }

    private void stopTorrentDownload(MessageReceivedEvent event, String torrentName) {
        if (!torrentName.isEmpty()) {
            SessionManager session = activeSessions.getOrDefault(torrentName, null);
            if (session != null) {
                session.pause();
                session.stop();
                activeSessions.remove(torrentName);
                NyaUtil.sendChannelMessage(event, "Stopped torrent: %s".formatted(torrentName));
                if (TorrentHelper.cleanupFiles(torrentName))
                    NyaUtil.sendChannelMessage(event, "Cleaned up torrent: %s".formatted(torrentName));
                else
                    NyaUtil.sendChannelMessage(event, "Error when cleaned up torrent: %s".formatted(torrentName));
            } else {
                NyaUtil.sendChannelMessage(event, "Torrent not found");
            }
        } else {
            event.getChannel().sendMessage("No torrent name provided!").queue();
        }
    }

    private void sendHelpMessage(MessageReceivedEvent event) {
        EmbedBuilder helpEmbed = new EmbedBuilder();
        helpEmbed.setTitle("Bot Commands");
        helpEmbed.setColor(Color.BLUE);
        helpEmbed.setDescription("Here are the available commands:");
        helpEmbed.addField("!ping", "Replies with 'Pong!'", false);
        helpEmbed.addField("!setup torrent", "Sech.", false);
        helpEmbed.addField("!help", "Displays this help message.", false);

        event.getChannel().sendMessageEmbeds(helpEmbed.build()).queue();
    }


    private void handlePruneCommand(MessageReceivedEvent event, String args) {
        if (args.isEmpty()) {
            event.getChannel().sendMessage("Please specify the number of messages to delete.").queue();
            return;
        }

        try {
            int amount = Integer.parseInt(args);

            if (amount < 1 || amount > 100) {
                event.getChannel().sendMessage("Please provide a number between 1 and 100.").queue();
                return;
            }

            event.getChannel().getHistory().retrievePast(amount + 1) // +1 to include command message
                 .queue(messages -> event.getChannel().purgeMessages(messages));

            event.getChannel().sendMessage("Deleted " + amount + " messages!").queue(
                    msg -> msg.delete().queueAfter(3, TimeUnit.SECONDS) // Auto-delete confirmation
            );

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("Invalid number! Please provide a valid integer.").queue();
        }
    }

    private boolean hasAdminPermission(MessageReceivedEvent event) {
        if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            return true;
        } else {
            event.getChannel().sendMessage("You don't have permission to use this command.").queue();
            return false;
        }
    }
}
