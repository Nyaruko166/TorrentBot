package me.nyaruko166.torrentskibidi;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Config {

    private static final Logger log = LogManager.getLogger(Config.class);
    private static final Gson gson = new Gson();
    private static final File configFile = new File("./libs/config.json");

    // Eager Singleton instance
    private static final Config instance = new Config();
    private static AppConfig appConfig;

    // Private constructor
    private Config() {
        if (!configFile.exists()) {
            try {
                log.info("Creating config file...");
                configFile.getParentFile().mkdirs();
                FileUtils.writeStringToFile(configFile, gson.toJson(AppConfig.configTemplate()), "UTF-8");
            } catch (IOException e) {
                log.error("Failed to create config file", e);
            }
        }
        File tempFolder = new File("./temp");
        if (!tempFolder.exists()) {
            tempFolder.mkdirs();
        }
        loadConfig();
    }

    // Method to load the configuration
    private void loadConfig() {
        try {
            appConfig = gson.fromJson(new FileReader(configFile), AppConfig.class);
            if (appConfig.getDiscordToken().isBlank()) {
                log.error("Discord token is blank");
                log.error("Please, put your Discord token in ./libs/config.json");
                System.exit(69);
            }
        } catch (FileNotFoundException e) {
            log.error("Config file not found {}", e.getMessage());
        }
    }

    // Method to update the configuration file
    public static void updateConfig() {
        try {
            FileUtils.writeStringToFile(configFile, gson.toJson(appConfig), "UTF-8");
            log.info("Configuration updated successfully.");
        } catch (IOException e) {
            log.error("Failed to update config file", e);
        }
    }

    // Combined method to access the AppConfig properties
    public static AppConfig getProperty() {
        return appConfig;
    }

}
