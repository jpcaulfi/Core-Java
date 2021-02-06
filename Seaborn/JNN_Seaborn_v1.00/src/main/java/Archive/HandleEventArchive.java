package Archive;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.math3.util.Precision;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class HandleEventArchive extends ListenerAdapter {

    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {

        String messageSent = event.getMessage().getContentRaw();
        String greeting = "morning";
        String[] headers = {"Here's the breakdown: ","Here's what I've got: ","What we're looking at: ","Here's the rundown: "};
        String randomHeader;
        String timeframe = "today";
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
        if (timeOfDay >= 12 && timeOfDay < 17) {
            greeting = "afternoon";
            timeframe = "tomorrow";
        } else if (timeOfDay >= 17) {
            greeting = "evening";
            timeframe = "tomorrow";
        }
        int rnd = new Random().nextInt(headers.length);
        randomHeader = headers[rnd] + "\r\n \r\n  ";
        String messageGreeting = "Good "+ greeting + ", traders. \r\n.\r\n" + randomHeader + "\r\n.";

        long startDate;
        long endDate;

        int rightNow = new DateTime().getHourOfDay();
        DateTime today = new DateTime().withTimeAtStartOfDay();
        DateTime tomorrow = today.plusDays(1).withTimeAtStartOfDay();
        DateTime afterTomorrow = tomorrow.plusDays(1).withTimeAtStartOfDay();
        if (rightNow < 12) {
            startDate = today.getMillis();
            endDate = tomorrow.getMillis();
        } else {
            startDate = tomorrow.getMillis();
            endDate = afterTomorrow.getMillis();
        }

        String messageRecapDescription = "Here's a performance recap of yesterday's catalyst tickers: \r\n.\r\n";

        String apiUrl = "https://rapidapi.p.rapidapi.com/market/get-earnings?region=US&startDate="+startDate+"&endDate="+endDate+"&size=10";

        HttpResponse<JsonNode> response = null;
        try {
            response = Unirest.get(apiUrl)
                    .header("x-rapidapi-host", "apidojo-yahoo-finance-v1.p.rapidapi.com")
                    .header("x-rapidapi-key", "**REDACTED_SECRET**")
                    .asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        List<String> earningsList = parseEarnings(response.getBody().toString());

        String messageEarnings = "Here are " + timeframe + "'s earnings releases: \r\n.\r\n.";
        String messageEarningsDescription = "I've listed each security with it's relative volume for your reference. \r\n.\r\n.";
        String messageSpecial = ".\r\n.\r\n ** NEW FEATURES COMING mother fuckers **";
        String messageClosing = ".\r\n.\r\n Good luck out there.";

        //String messageFull = messageGreeting+messageEarnings+messageDescription+earningsList;
        //for (int i = 0; i < earningsList.size(); i++) {
        //    messageFull += earningsList.get(i);
        //}
        //messageFull += messageSpecial+messageClosing;
        //if(messageSent.equalsIgnoreCase("Seaborn run")){
        //    event.getChannel().sendMessage(messageFull).queue();
        //}

        if(messageSent.equalsIgnoreCase("Seaborn run")){
            //event.getChannel().sendMessage(messageGreeting).queue();
            //event.getChannel().sendMessage(messageEarnings).queue();
            //event.getChannel().sendMessage(messageEarningsDescription).queue();
            for (int i = 0; i < earningsList.size(); i++) {
                //event.getChannel().sendMessage(earningsList.get(i)).queue();
            }
            //event.getChannel().sendMessage(messageSpecial).queue();
            //event.getChannel().sendMessage(messageClosing).queue();

            EmbedBuilder info = new EmbedBuilder();
            info.setTitle(messageGreeting);
            info.setDescription(messageEarnings + messageEarningsDescription);
            for (int i = 0; i < earningsList.size(); i++) {
                info.addField("-", earningsList.get(i), false);
            }
            info.setColor(0x8056b8);
            info.setFooter(messageClosing);

            event.getChannel().sendTyping().queue();
            event.getChannel().sendMessage(info.build()).queue();
            info.clear();
        }

    }

    public static List<String> parseEarnings(String responseBody) {

        List<String> earningsList = new ArrayList<String>();
        JSONObject jsonE = new JSONObject(responseBody.toString());
        JSONObject financeE = jsonE.getJSONObject("finance");
        JSONArray resultE = financeE.getJSONArray("result");
        for (int i = 0; i < resultE.length(); i++) {
            JSONObject outputE = resultE.getJSONObject(i);
            String ticker = outputE.getString("ticker");
            String shortName = outputE.getString("companyShortName");
            long releaseTimeLong = outputE.getLong("startDateTime");
            DateTime releaseTime = new DateTime(releaseTimeLong);
            double relativeVolume = parseVolume(ticker);
            String volumeSuffix;
            if (relativeVolume < 0) {
                volumeSuffix = "";
            } else {
                volumeSuffix = "+";
            }
            // We can place a simple if statement here to establish a minimum relative volume threshold
            earningsList.add(".\r\n" + "$" + ticker + "\r\n" + shortName + "\r\n" + releaseTime.toString(DateTimeFormat.fullDateTime()) + "\r\n Relative volume: " + volumeSuffix + relativeVolume + "% \r\n.");
        }

        return earningsList;
    }

    public static double parseVolume(String symbol) {

        String apiUrl = "https://apidojo-yahoo-finance-v1.p.rapidapi.com/stock/v2/get-analysis?region=US&symbol="+symbol;

        HttpResponse<JsonNode> response = null;
        try {
            response = Unirest.get(apiUrl)
                    .header("x-rapidapi-host", "apidojo-yahoo-finance-v1.p.rapidapi.com")
                    .header("x-rapidapi-key", "**REDACTED_SECRET**")
                    .asJson();
        } catch (
                UnirestException e) {
            e.printStackTrace();
        }

        JSONObject json = new JSONObject(response.getBody().toString());
        JSONObject summary = json.getJSONObject("summaryDetail");
        JSONObject volumeDetail = summary.getJSONObject("volume");
        double volume = volumeDetail.getDouble("raw");
        JSONObject volume10Detail = summary.getJSONObject("averageDailyVolume10Day");
        double volume10 = volume10Detail.getDouble("raw");
        JSONObject volume3MDetail = summary.getJSONObject("averageVolume");
        double volume3M = volume3MDetail.getDouble("raw");
        // Going to use the 3-Month volume for now
        double relativeVolume = Precision.round(((volume - volume3M) / volume3M * 100), 3);

        return relativeVolume;
    }

}