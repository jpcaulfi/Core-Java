import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;

import static Events.Catalysts.sendCatalysts;
import static Events.Options.sendOptions;
import static Events.Recaps.sendRecaps;


public class Main {

    public static void main(String[] args) throws Exception {

        try {
            JDA jda = JDABuilder.createDefault("**REDACTED_SECRET**").build();
            jda.awaitReady();
            String channelName = "**REDACTED_SECRET**";

            List<TextChannel> channels = jda.getTextChannelsByName(channelName, true);
            for (TextChannel ch : channels) {
                sendRecaps(ch, 1);
                sendCatalysts(ch, 1);
                sendOptions(ch, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.exit(0);
    }
}