package Events;

import Data.EarningsRecord;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import org.joda.time.format.DateTimeFormat;

public class Options {

    public static void sendOptions(TextChannel ch, int switchDB) throws Exception {

        String messageDescriptionOptions = "Coming soon to Seaborn version 1.10. This embed will provide potential options plays based on technical analysis on the daily chart. It will prioritize affordable options (ie. < $100) and be intended for trading options over 2 weeks to expiration.";
        String messageClosing = "Big things coming...";

        EmbedBuilder info = new EmbedBuilder();
        info.setTitle("** Options Plays **");
        info.setDescription(messageDescriptionOptions);
        info.addField("-", "** CALLS **", false);
        info.addField("-", "-", false);
        info.addField("-", "** PUTS **", false);
        info.addField("-", "-", false);
        info.setFooter(messageClosing);
        info.setColor(0x32a858);
        ch.sendMessage(info.build()).queue();
        info.clear();

    }

}
