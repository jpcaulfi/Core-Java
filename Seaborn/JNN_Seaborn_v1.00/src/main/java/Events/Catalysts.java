package Events;

import Data.EarningsRecord;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.math3.util.Precision;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.*;

public class Catalysts {


    public static void sendCatalysts(TextChannel ch, int switchDB) throws Exception {

        // Deciding whether to show today's or tomorrow's catalysts

        long startDate;
        long endDate;
        String timeframe;
        int rightNow = new DateTime().getHourOfDay();
        DateTime today = new DateTime().withTimeAtStartOfDay();
        DateTime tomorrow = today.plusDays(1).withTimeAtStartOfDay();
        DateTime afterTomorrow = tomorrow.plusDays(1).withTimeAtStartOfDay();
        if (rightNow < 17) {
            startDate = today.getMillis();
            endDate = tomorrow.getMillis();
            timeframe = "today";
        } else {
            startDate = tomorrow.getMillis();
            endDate = afterTomorrow.getMillis();
            timeframe = "tomorrow";
        }
        String messageDescriptionMain = "Here are " + timeframe + "'s catalysts. I've listed each security ranked in order of highest relative volume.";
        String messageClosing = "Now go make some money.";

        // Earnings method call

        String apiUrl = "https://rapidapi.p.rapidapi.com/market/get-earnings?region=US&startDate=" + startDate + "&endDate=" + endDate + "&size=25";
        HttpResponse<JsonNode> responseEarnings = null;
        try {
            responseEarnings = Unirest.get(apiUrl)
                    .header("x-rapidapi-host", "apidojo-yahoo-finance-v1.p.rapidapi.com")
                    .header("x-rapidapi-key", "**REDACTED_SECRET**")
                    .asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        assert responseEarnings != null;
        JSONObject jsonEarnings = new JSONObject(responseEarnings.getBody().toString());
        List<EarningsRecord> earningsList = retrieveEarnings(jsonEarnings);

        // Event Reader (message), embed builder, send embed

        EmbedBuilder info = new EmbedBuilder();
        info.setTitle("** Upcoming Catalysts **");
        info.setDescription(messageDescriptionMain);
        info.addField("-", "** EARNINGS RELEASES **", false);
        for (EarningsRecord row : earningsList) {
            String rowMessage = "$" + row.getTicker() + "\r\n" + row.getShortName() + "\r\n" + row.getReleaseTime().toString(DateTimeFormat.fullDateTime()) + "\r\n" + "  Average prediction for earnings amongst " + row.getNumberOfAnalysts() + " analysts is about " + row.getPrediction() + "\r\n  Relative volume: " + row.getVolumeSuffix() + row.getRelativeVolume() + "%\r\n" + row.getChartUrl();
            info.addField("-", rowMessage, false);
            if (switchDB == 1) {
                insertEarnings(row.getReleaseTime().toString("yyyy-MM-dd"), row.getTicker(), row.getShortName(), row.getReleaseTime(), row.getNumberOfAnalysts(), row.getPrediction(), row.getVolumeSuffix(), row.getRelativeVolume(), row.getChartUrl());
            }
        }
        //info.addField("-", "** OTHER CATALYSTS **", false);
        //info.addField("-", "Coming Soon", false);
        info.setFooter(messageClosing);
        info.setColor(0x3badcc);
        ch.sendMessage(info.build()).queue();
        info.clear();

    }


    public static List<EarningsRecord> retrieveEarnings(JSONObject jsonEarnings) {
        List<EarningsRecord> earningsList = new ArrayList<>();
        JSONObject financeE = jsonEarnings.getJSONObject("finance");
        JSONArray resultE = financeE.getJSONArray("result");
        for (int i = 0; i < resultE.length(); i++) {
            JSONObject outputE = resultE.getJSONObject(i);
            String ticker = outputE.getString("ticker");
            String shortName = outputE.getString("companyShortName").replace("'", "");
            long releaseTimeLong = outputE.getLong("startDateTime");
            DateTime releaseTime = new DateTime(releaseTimeLong);

            // Analyses method calls
            String apiUrl = "https://apidojo-yahoo-finance-v1.p.rapidapi.com/stock/v2/get-analysis?region=US&symbol=" + ticker;
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
            assert response != null;
            JSONObject jsonAnalysis = new JSONObject(response.getBody().toString());
            int numberOfAnalysts = parseNumberOfAnalysts(jsonAnalysis);
            double prediction = parsePredictions(jsonAnalysis);
            double relativeVolume = parseVolume(jsonAnalysis);
            String volumeSuffix;
            if (relativeVolume < 0) {
                volumeSuffix = "";
            } else {
                volumeSuffix = "+";
            }
            String chartUrl = parseChart(jsonAnalysis, ticker);
            if (relativeVolume > -25){
                earningsList.add(new EarningsRecord(ticker, shortName, releaseTime, numberOfAnalysts, prediction, volumeSuffix, relativeVolume, chartUrl));
            }
        }

        //Sorting the earningsList by order of largest relativeVolume
        earningsList.sort(new Comparator<EarningsRecord>() {
            @Override
            public int compare(EarningsRecord c1, EarningsRecord c2) {
                return Double.compare(c2.getRelativeVolume(), c1.getRelativeVolume());
            }
        });
        return earningsList;
    }


    public static double parseVolume(JSONObject jsonAnalysis) {
        JSONObject summary = jsonAnalysis.getJSONObject("summaryDetail");
        JSONObject volumeDetail = summary.getJSONObject("volume");
        double volume = volumeDetail.getDouble("raw");
        //JSONObject volume10Detail = summary.getJSONObject("averageDailyVolume10Day");
        //double volume10 = volume10Detail.getDouble("raw");
        JSONObject volume3MDetail = summary.getJSONObject("averageVolume");
        double volume3M = volume3MDetail.getDouble("raw");
        // Going to use the 3-Month volume for now
        return Precision.round(((volume - volume3M) / volume3M * 100), 3);
    }

    public static int parseNumberOfAnalysts(JSONObject jsonAnalysis) {
        try {
            JSONObject earningsTrend = jsonAnalysis.getJSONObject("earningsTrend");
            JSONArray trend = earningsTrend.getJSONArray("trend");
            JSONObject trend0 = trend.getJSONObject(0);
            JSONObject earningsEstimate = trend0.getJSONObject("earningsEstimate");
            JSONObject analysts = earningsEstimate.getJSONObject("numberOfAnalysts");
            return analysts.getInt("raw");
        } catch (JSONException e) {
            return 0;
        }
    }

    public static double parsePredictions(JSONObject jsonAnalysis) {
        try {
            JSONObject earningsTrend = jsonAnalysis.getJSONObject("earningsTrend");
            JSONArray trend = earningsTrend.getJSONArray("trend");
            JSONObject trend0 = trend.getJSONObject(0);
            JSONObject earningsEstimate = trend0.getJSONObject("earningsEstimate");
            JSONObject avg = earningsEstimate.getJSONObject("avg");
            //JSONObject analysts = earningsEstimate.getJSONObject("numberOfAnalysts");
            //int numberOfAnalysts = analysts.getInt("raw");
            return avg.getDouble("raw");
        } catch (JSONException e) {
            return 0;
        }
    }

    public static String parseChart(JSONObject jsonAnalysis, String symbol) {
        JSONObject jsonPrice = jsonAnalysis.getJSONObject("price");
        String exchange = jsonPrice.getString("exchange");
        String exchangeKey;
        if (exchange.equals("NYQ")) {
            exchangeKey = "NYSE";
        } else if (exchange.equals("NMS")) {
            exchangeKey = "NASDAQ";
        } else {
            exchangeKey = exchange;
        }
        return "https://www.tradingview.com/symbols/" + exchangeKey + "-" + symbol + "/";
    }

    public static void insertEarnings(String date, String ticker, String shortName, DateTime releaseTime, int numberOfAnalysts, double prediction, String volumeSuffix, double relativeVolume, String chartUrl) throws Exception {
        try {
            Connection conn = getDBConnection();
            assert conn != null;
            PreparedStatement posted = conn.prepareStatement("INSERT INTO earnings (date, ticker, shortName, releaseTime, numberOfAnalysts, prediction, volumeSuffix, relativeVolume, chartUrl) VALUES ('"+date+"', '"+ticker+"', '"+shortName+"', '"+releaseTime+"', '"+numberOfAnalysts+"', '"+prediction+"', '"+volumeSuffix+"', '"+relativeVolume+"', '"+chartUrl+"')");
            posted.executeUpdate();
        } catch(Exception e){
            System.out.println(e);
        }

    }

    public static Connection getDBConnection() throws Exception {

        try {
            String driver = "com.mysql.cj.jdbc.Driver";
            String url = "jdbc:mysql://localhost:3306/seaborn";
            String username = "root";
            String password = "Karrot$42";
            Class.forName(driver);

            return DriverManager.getConnection(url, username, password);
        } catch(Exception e){
            System.out.println(e);
        }

        return null;
    }

}