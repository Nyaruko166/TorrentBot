package me.nyaruko166.torrentskibidi.discord;

import me.nyaruko166.torrentskibidi.Config;
import me.nyaruko166.torrentskibidi.discord.Listener.MessageReceiveListener;
import me.nyaruko166.torrentskibidi.discord.Listener.TestListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumSet;

public class Bot {

    static Logger log = LogManager.getLogger(Bot.class);

    private static final String DISCORD_TOKEN = Config
            .getProperty().getDiscordToken();
    public static JDA api;

    public static void runBot() {
        log.info("Bot is starting...");
        api = JDABuilder.createLight(DISCORD_TOKEN, EnumSet.of(GatewayIntent.GUILD_MESSAGES,
                                GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.MESSAGE_CONTENT))
                        .setActivity(Activity.of(Activity.ActivityType.CUSTOM_STATUS, "Just a random bot passing through."))
                        .addEventListeners(new MessageReceiveListener())
                        .build();
    }

//    public static void sendClip(String id) {
//        api.getGuildById(Config.getProperty().getGuildId())
//           .getTextChannelById(Config.getProperty().getChannelId())
//           .sendMessage("https://youtu.be/" + id).queue();
//    }
//    public static void main(String[] args) {
//        JDA jda = JDABuilder.createLight(DISCORD_TOKEN, EnumSet.of(GatewayIntent.GUILD_MESSAGES,
//                        GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.MESSAGE_CONTENT))
//                .setActivity(Activity.of(Activity.ActivityType.WATCHING, "Just a random bot passing through."))
//                .addEventListeners(new MessageReceiveListener())
//                .addEventListeners(new SlashCommandListener())
//                .build();

    // Register your commands to make them visible globally on Discord:
//        CommandListUpdateAction commands = jda.updateCommands();

    // Add all your commands on this action instance
//        commands.addCommands(
//                Commands.slash("say", "Makes the bot say what you tell it to")
//                        .addOption(OptionType.STRING, "content", "What the bot should say", true), // Accepting a user input
//                Commands.slash("leave", "Makes the bot leave the server")
//                        .setGuildOnly(true) // this doesn't make sense in DMs
//                        .setDefaultPermissions(DefaultMemberPermissions.DISABLED) // only admins should be able to use this command.
//        );

    // Then finally send your commands to discord using the API
//        commands.queue();
//    }

}
