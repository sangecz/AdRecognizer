package cz.sange.adrecognizer.srt;

/**
 * Created by sange on 16/11/15.
 */
public class SrtRecord {

    private StringBuilder stringBuilder;
    private int frameNum;
    private String timeStamp;
    private String msg;

//    public SrtRecord() {
//        stringBuilder = new StringBuilder();
//    }

    public SrtRecord(int frameNum, String timeStamp, String msg) {
        stringBuilder = new StringBuilder();

        this.frameNum = frameNum;
        this.timeStamp = timeStamp;
        this.msg = msg;

        buildRecord();
    }

    private void buildRecord() {
        stringBuilder.append(frameNum);
        stringBuilder.append("\n");
        stringBuilder.append(timeStamp);
        stringBuilder.append("\n");
        stringBuilder.append(msg);
        stringBuilder.append("\n");
    }

    @Override
    public String toString() {
        return stringBuilder.toString();
    }
}
