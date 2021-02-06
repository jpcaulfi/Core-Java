package Events;

import Data.EarningsRecapRecord;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.math3.util.Precision;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.*;
import java.util.*;

public class Recaps {

    public static void sendRecaps(TextChannel ch, int switchDB) throws Exception {

        // Configuring what day to recap
        long startDate;
        long endDate;
        String recapDate;
        String timeframe;
        int gainIndex;
        int rightNow = new DateTime().getHourOfDay();
        DateTime today = new DateTime().withTimeAtStartOfDay();
        DateTime yesterday = today.plusDays(-1).withTimeAtStartOfDay();
        DateTime tomorrow = today.plusDays(1).withTimeAtStartOfDay();
        if (rightNow < 17) {
            recapDate = yesterday.toString( "yyyy-MM-dd");
            timeframe = "yesterday";
            startDate = yesterday.getMillis();
            endDate = today.getMillis();
            gainIndex = 3;
        } else {
            recapDate = today.toString( "yyyy-MM-dd");
            timeframe = "today";
            startDate = today.getMillis();
            endDate = tomorrow.getMillis();
            gainIndex = 4;
        }

        List<EarningsRecapRecord> earningsRecapList = retrieveRecaps(recapDate, startDate, endDate, gainIndex);
        String messageDescriptionMain = "Here's a recap of "+ timeframe +"'s catalysts. I've included some details and ranked them by largest market movement.";
        EmbedBuilder info = new EmbedBuilder();
        info.setTitle("** Recap **");
        info.setDescription(messageDescriptionMain);
        info.addField("-", "** EARNINGS RELEASES **", false);
        for (EarningsRecapRecord row : earningsRecapList) {
            String rowMessage = "$" + row.getTicker() + "\r\n" + row.getShortName() + "\r\n" + "  Predicted: " + row.getPrediction() + "\r\n" + "  Actual: " + "coming-v1.01" + "\r\n" + "     Surprise: " + "coming-v1.01" + "\r\n" + "\r\n" + " Gain: " + row.getGainSuffix() + row.getGain() + "%" + "\r\n" + row.getChartUrl();
            info.addField("-", rowMessage, false);
            String keyDate = row.getReleaseTime().split(" ")[0];
            if (switchDB == 1) {
                insertEarningsRecap(keyDate, row.getTicker(), row.getShortName(), row.getReleaseTime(), row.getNumberOfAnalysts(), row.getPrediction(), row.getVolumeSuffix(), row.getRelativeVolume(), row.getChartUrl(), row.getActual(), row.getDifferentialSuffix(), row.getDifferential(), row.getGainSuffix(), row.getGain());
            }
        }
        //info.addField("-", "** OTHER CATALYSTS **", false);
        //info.addField("-", "Coming Soon", false);
        info.setColor(0xdb3d7a);
        ch.sendMessage(info.build()).queue();
        info.clear();

    }

    public static List<EarningsRecapRecord> retrieveRecaps(String recapDate, long startDate, long endDate, int gainIndex) throws Exception {

        // Making initial call to the API for the EARNINGS RECAP JSON
        String apiUrlER = "https://rapidapi.p.rapidapi.com/market/get-earnings?region=US&startDate="+startDate+"&endDate="+endDate+"&size=25";
        HttpResponse<JsonNode> responseRecaps = null;
        try {
            responseRecaps = Unirest.get(apiUrlER)
                    .header("x-rapidapi-host", "apidojo-yahoo-finance-v1.p.rapidapi.com")
                    .header("x-rapidapi-key", "**REDACTED_SECRET**")
                    .asJson();
        } catch (
                UnirestException e) {
            e.printStackTrace();
        }
        JSONObject jsonRecaps = new JSONObject(responseRecaps.getBody().toString());
        JSONObject financeR = jsonRecaps.getJSONObject("finance");
        JSONArray resultR = financeR.getJSONArray("result");

        // Pulling the necessary data from the database
        Connection conn = getDBConnection();
        assert conn != null;
        PreparedStatement statement = conn.prepareStatement("SELECT * FROM earnings WHERE date = '" + recapDate + "'");
        ResultSet result = statement.executeQuery();
        List<EarningsRecapRecord> earningsRecapList = new ArrayList<>();
        while(result.next()){
            double actual = 0;
            double differential = 0;
            String symbol = result.getString("ticker");
            double prediction = result.getDouble("prediction");
            for (int i = 0; i < resultR.length(); i++) {
                JSONObject outputR = resultR.getJSONObject(i);
                if (outputR.getString("ticker").equals(symbol)){
                    //differential = outputR.getDouble("surprisePercent");
                    //actual = Precision.round((((differential * prediction) / 100) + prediction), 3);
                    differential = 0;
                    actual = 0;
                }
            }
            double gain = parseGain(symbol, gainIndex);
            String differentialSuffix;
            if (differential < 0) {
                differentialSuffix = "";
            } else {
                differentialSuffix = "+";
            }
            String gainSuffix;
            if (gain < 0) {
                gainSuffix = "";
            } else {
                gainSuffix = "+";
            }
            earningsRecapList.add(new EarningsRecapRecord(result.getString("ticker"),result.getString("shortName"),result.getString("releaseTime"),result.getInt("numberOfAnalysts"),result.getDouble("prediction"),result.getString("volumeSuffix"),result.getDouble("relativeVolume"),result.getString("chartUrl"),actual,differentialSuffix,differential,gainSuffix,gain));
        }

        //Sorting the earningsRecapList by order of largest gain
        earningsRecapList.sort(new Comparator<EarningsRecapRecord>() {
            @Override
            public int compare(EarningsRecapRecord c1, EarningsRecapRecord c2) {
                return Double.compare(Math.abs(c2.getGain()), Math.abs(c1.getGain()));
            }
        });

        return earningsRecapList;
    }

    public static double parseGain(String symbol, int gainIndex) {

        // Making another call to the API for the MARKET MOVEMENT JSON
        String apiUrlMOVE = "https://rapidapi.p.rapidapi.com/stock/v2/get-chart?interval=1d&symbol="+symbol+"&range=5d&region=US";
        HttpResponse<JsonNode> responseMOVE = null;
        try {
            responseMOVE = Unirest.get(apiUrlMOVE)
                    .header("x-rapidapi-host", "apidojo-yahoo-finance-v1.p.rapidapi.com")
                    .header("x-rapidapi-key", "**REDACTED_SECRET**")
                    .asJson();
        } catch (
                UnirestException e) {
            e.printStackTrace();
        }
        assert responseMOVE != null;
        JSONObject jsonMOVE = new JSONObject(responseMOVE.getBody().toString());
        JSONObject chartMOVE = jsonMOVE.getJSONObject("chart");
        JSONArray resultMOVE = chartMOVE.getJSONArray("result");
        JSONObject result = resultMOVE.getJSONObject(0);
        JSONObject indicators = result.getJSONObject("indicators");
        JSONArray quote = indicators.getJSONArray("quote");
        JSONObject quoteCore = quote.getJSONObject(0);
        JSONArray getOpens = quoteCore.getJSONArray("open");
        JSONArray getHighs = quoteCore.getJSONArray("high");
        try {
            double prevOpen = getOpens.getDouble(gainIndex);
            double prevHigh = getHighs.getDouble(gainIndex);

            return Precision.round((prevHigh - prevOpen) / prevOpen * 100, 3);
        } catch (JSONException e) {
            return 0;
        }
    }

    public static void insertEarningsRecap(String date, String ticker, String shortName, String releaseTime, int numberOfAnalysts, double prediction, String volumeSuffix, double relativeVolume, String chartUrl, double actual, String differentialSuffix, double differential, String gainSuffix, double gain) {
        try {
            Connection conn = getDBConnection();
            assert conn != null;
            PreparedStatement posted = conn.prepareStatement("INSERT INTO earningsrecap (date, ticker, shortName, releaseTime, numberOfAnalysts, prediction, volumeSuffix, relativeVolume, chartUrl, actual, differentialSuffix, differential, gainSuffix, gain) VALUES ('"+date+"', '"+ticker+"', '"+shortName+"', '"+releaseTime+"', '"+numberOfAnalysts+"', '"+prediction+"', '"+volumeSuffix+"', '"+relativeVolume+"', '"+chartUrl+"', '"+actual+"', '"+differentialSuffix+"', '"+differential+"', '"+gainSuffix+"', '"+gain+"')");
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
            String password = "**REDACTED_SECRET**";
            Class.forName(driver);

            return DriverManager.getConnection(url, username, password);
        } catch(Exception e){
            System.out.println(e);
        }

        return null;
    }
}
