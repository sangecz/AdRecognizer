package cz.sange.adrecognizer.dtw;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sange on 05/01/16.
 */
public class DTWRunnable implements Runnable {
    int threadNo = -1 ;
    List<String> list = new ArrayList<String>();
    private byte [] row;
    private byte [] newRow;

    public DTWRunnable(List list, int threadNo ) {
        this.list.addAll(list);
        this.threadNo =threadNo;
    }

    public void run() {
        for (int i = 0; i < list.size(); i++) {
            String element = list.get(i);
            System.out.println("By Thread:" + threadNo+", Processed Element:" +element);
//            list.set(i, element + " " + threadNo);
        }
    }


}
