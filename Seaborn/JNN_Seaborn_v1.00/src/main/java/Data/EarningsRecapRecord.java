package Data;

import org.joda.time.DateTime;

public class EarningsRecapRecord {

    String ticker;
    String shortName;
    String releaseTime;
    int numberOfAnalysts;
    double prediction;
    String volumeSuffix;
    double relativeVolume;
    String chartUrl;
    double actual;
    String differentialSuffix;
    double differential;
    String gainSuffix;
    double gain;

    public EarningsRecapRecord(String ticker, String shortName, String releaseTime, int numberOfAnalysts, double prediction, String volumeSuffix, double relativeVolume, String chartUrl, double actual, String differentialSuffix, double differential, String gainSuffix, double gain) {
        this.ticker = ticker;
        this.shortName = shortName;
        this.releaseTime = releaseTime;
        this.numberOfAnalysts = numberOfAnalysts;
        this.prediction = prediction;
        this.volumeSuffix = volumeSuffix;
        this.relativeVolume = relativeVolume;
        this.chartUrl = chartUrl;
        this.actual = actual;
        this.differentialSuffix = differentialSuffix;
        this.differential = differential;
        this.gainSuffix = gainSuffix;
        this.gain = gain;
    }

    public String getTicker() {
        return ticker;
    }

    public String getShortName() {
        return shortName;
    }

    public String getReleaseTime() {
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

    public double getActual() {
        return actual;
    }

    public String getDifferentialSuffix() {
        return differentialSuffix;
    }

    public double getDifferential() {
        return differential;
    }

    public String getGainSuffix() {
        return gainSuffix;
    }

    public double getGain() {
        return gain;
    }

    public void setActual(double actual) {
        this.actual = actual;
    }

    public void setDifferential(double differential) {
        this.differential = differential;
    }

    public void setGain(double gain) {
        this.gain = gain;
    }
}
