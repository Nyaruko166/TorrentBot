package me.nyaruko166.torrentskibidi.discord;

import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Log4j2
public class NyaUtil {

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static void sendPrivateMessage(MessageReceivedEvent event, String message) {
        event.getAuthor()
             .openPrivateChannel()
             .flatMap(privateChannel -> privateChannel.sendMessage(message))
             .queue();
    }

    public static void sendChannelMessage(MessageReceivedEvent event, String message) {
        event.getChannel().sendMessage(message).queue();
    }

    public static boolean compressFolder(File zipFile, String password, File folder) {
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setEncryptionMethod(EncryptionMethod.AES);
        try {
            new ZipFile(zipFile, password.toCharArray())
                    .addFolder(folder, zipParameters);
            ZipFile zip = new ZipFile(zipFile);
            if (zipFile.exists() && zipFile.isFile()) {
                zip.setComment("https://youtu.be/dQw4w9WgXcQ?si=_qGu3O2i7NvkhjN5\n" +
                               "https://youtu.be/ifs4zmWD3ms?si=hjo9KhPLVi05vCsW\n" +
                               "https://youtu.be/IzSYlr3VI1A?si=L0rD1Evm8AFVA3Tf\n" +
                               "https://www.youtube.com/@Nyaruko166\n" +
                               "Created by Nyaruko166.");
                return true;
            }
            return false;

        } catch (ZipException e) {
            log.error("Error when compress folder", e);
            return false;
        }
    }

    public static String getTimeStamp(MessageReceivedEvent event) {
        return String.format("%s - %s",
                event.getJDA().getSelfUser().getName(),
                LocalDateTime.now().format(timeFormatter));
    }
}
