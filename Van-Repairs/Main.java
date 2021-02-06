import com.opencsv.CSVWriter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.FileWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends ListenerAdapter {

    public static void main(String[] args) {

        String introMessage = "Hello drivers, this is our chat room for van repairs. " +
                "When you notice that your van requires maintenance of any kind, " +
                "please type a message into this chat and our Van Repairs Bot will keep " +
                "a record of it so we can get it repaired ASAP. Thanks!";

        try {
            JDA jda = JDABuilder.createDefault("** REDACTED SECRET KEY **").build();
            jda.addEventListener(new Main());
            jda.awaitReady();
            String channelName = "van_repairs";

            List<TextChannel> channels = jda.getTextChannelsByName(channelName, true);
            for (TextChannel ch : channels) {
                ch.sendMessage(introMessage).queue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.exit(0);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        User author = event.getAuthor();
        Message message = event.getMessage();
        MessageChannel channel = event.getChannel();

        if (!author.getName().equals("RPME_Van_Repairs")) {

            String repairMessage = message.getContentDisplay();

            String[] popWords = new String[] {"Van", "van", "has", "is", "on", "in", "a"};
            String vanNumber = "Unknown Number";
            Pattern p = Pattern.compile("\\d{3}");
            Matcher m = p.matcher(repairMessage);
            while(m.find()) {
                vanNumber = m.group();
            }

            for (String word : popWords) {
                String regex = "\\s*\\b" + word + "\\b\\s*";
                repairMessage = repairMessage.replaceAll(regex, " ");
            }
            repairMessage = repairMessage.replaceAll(vanNumber, " ");
            repairMessage = repairMessage.replaceAll("\\s++", " ");

            String msg = "Got it, " + author.getName() + ", I've made a record for Van " + vanNumber + ":  '" + repairMessage + "'";
            channel.sendMessage(msg).queue();

            try {
                writeToCsv(vanNumber, repairMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }


    }

    public void writeToCsv(String vanNumber, String message) throws Exception {

        String csvPath = "** REDACTED FILE PATH **";
        String csvFile = "VanRepairs.csv";

        CSVWriter writer = new CSVWriter(new FileWriter(csvPath+csvFile, true));
        String[] repairRecord = new String[2];
        repairRecord[0] = vanNumber;
        repairRecord[1] = message;
        writer.writeNext(repairRecord);
        writer.close();

    }
}