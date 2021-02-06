package Data;

import org.joda.time.DateTime;

public class EarningsRecord {

    String ticker;
    String shortName;
    DateTime releaseTime;
    int numberOfAnalysts;
    double prediction;
    String volumeSuffix;
    double relativeVolume;
    String chartUrl;
    double actual;
    double differential;
    double gain;

    public EarningsRecord(String ticker, String shortName, DateTime releaseTime, int numberOfAnalysts, double prediction, String volumeSuffix, double relativeVolume, String chartUrl) {
        this.ticker = ticker;
        this.shortName = shortName;
        this.releaseTime = releaseTime;
        this.numberOfAnalysts = numberOfAnalysts;
        this.prediction = prediction;
        this.volumeSuffix = volumeSuffix;
        this.relativeVolume = relativeVolume;
        this.chartUrl = chartUrl;
        this.actual = 0;
        this.differential = 0;
        this.gain = 0;
    }

    public String getTicker() {
        return ticker;
    }

    public String getShortName() {
        return shortName;
    }

    public DateTime getReleaseTime() {
        return releaseTime;
    }

    public int getNumberOfAnalysts() {
        return numberOfAnalysts;
    }

    public double getPrediction() {
        return prediction;
    }

    public String getVolumeSuffix() {
        return volumeSuffix;
    }

    public double getRelativeVolume() {
        return relativeVolume;
    }

    public String getChartUrl() {
        return chartUrl;
    }

}
