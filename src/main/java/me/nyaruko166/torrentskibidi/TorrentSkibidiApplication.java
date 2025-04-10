package me.nyaruko166.torrentskibidi;

import me.nyaruko166.torrentskibidi.discord.Bot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TorrentSkibidiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TorrentSkibidiApplication.class, args);
        Bot.runBot();
    }

}
