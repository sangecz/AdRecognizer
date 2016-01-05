package cz.sange.adrecognizer.dtw;

import cz.sange.adrecognizer.bean.MainManagedBean;

import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * Created by sange on 05/01/16.
 */
public class DTWCallable implements Callable<DTWResult> {
    private int threadNo = -1 ;
    private byte [] delimiterData;
    private byte [] videoData;
    private int [] row;
    private int [] newRow;
    private int start;
    private int end;
    private final MainManagedBean mainManagedBean;

    public DTWCallable(int threadNo, MainManagedBean mainManagedBean, byte[] delimiterData, byte[] videoData, int start, int end) {
        this.mainManagedBean = mainManagedBean;
        this.threadNo = threadNo;
        this.delimiterData = delimiterData;
        this.videoData = videoData;
        this.start = start;
        // allow other chunks to overlap, except the last one
        this.end = (end == videoData.length ? end - delimiterData.length : end);
        row = new int[delimiterData.length];
        newRow = new int[delimiterData.length];
    }

    public DTWResult call() throws Exception {

        int minDtw = Integer.MAX_VALUE;
        int dist;
        int position = 0;
        byte [] sample;

        int x = 1; // every x-th element of videoData
        for (int i = start; i < end; i += x) {
            sample = Arrays.copyOfRange(videoData, i, i + delimiterData.length);
            dist = DTWDistance(sample);
            if(dist < minDtw) {
                minDtw = dist;
                position = i;
            }
            synchronized (mainManagedBean) {
                mainManagedBean.setProcessed(i - start, threadNo);
            }
        }
        return new DTWResult(minDtw, position);
    }

    private int DTWDistance(byte [] sample) {

        for (int i = 1; i < delimiterData.length; i++) {
            row[i] = Integer.MAX_VALUE;
        }
        row[0] = 0;

        for (int i = 1; i < delimiterData.length; i++) {
            newRow[0] = Integer.MAX_VALUE;
            for (int j = 1; j < sample.length; j++) {
                int cost =  Math.abs(delimiterData[i] - sample[j]);
                newRow[j] =  cost + Math.min(           row[j],    // insertion
                        Math.min(                       newRow[j - 1],    // deletion
                                                        row[j - 1]));    // match
            }
            int [] tmp = newRow;
            newRow = row;
            row = tmp;
        }

        return row[delimiterData.length - 1];
    }
}
