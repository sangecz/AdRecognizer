package cz.sange.adrecognizer.dtw;

/**
 * Created by sange on 05/01/16.
 */
public class DTWResult {

    private int minDtw;
    private int position;
    private int delimiterLenBytes;

    public DTWResult(int minDtw, int position, int delimiterLenBytes) {
        this.minDtw = minDtw;
        this.position = position;
        this.delimiterLenBytes = delimiterLenBytes;
    }

    public int getDelimiterLenBytes() {
        return delimiterLenBytes;
    }

    public void setDelimiterLenBytes(int delimiterLenBytes) {
        this.delimiterLenBytes = delimiterLenBytes;
    }

    public int getMinDtw() {
        return minDtw;
    }

    public void setMinDtw(int minDtw) {
        this.minDtw = minDtw;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
