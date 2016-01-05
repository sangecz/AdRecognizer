package cz.sange.adrecognizer.dtw;

/**
 * Created by sange on 05/01/16.
 */
public class DTWResult {

    private int minDtw;
    private int position;

    public DTWResult(int minDtw, int position) {
        this.minDtw = minDtw;
        this.position = position;
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
