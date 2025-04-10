package me.nyaruko166.torrentskibidi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AppConfig {

    private String discordToken;

    private String guildId;

    private String channelId;

    public static AppConfig configTemplate() {
        return AppConfig.builder().discordToken(" ").guildId(" ").channelId(" ")
                        .build();
    }
}
