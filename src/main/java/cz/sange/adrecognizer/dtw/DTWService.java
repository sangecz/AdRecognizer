package cz.sange.adrecognizer.dtw;

import cz.sange.adrecognizer.bean.MainManagedBean;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by sange on 05/01/16.
 */
public class DTWService {

    private byte [] videoData;
    private MainManagedBean mainManagedBean;

    public DTWService(byte[] videoData, MainManagedBean bean) {
        this.videoData = videoData;
        this.mainManagedBean = bean;
    }

    public DTWResult start(byte[] delimiterData) {

        ExecutorService executor = Executors.newWorkStealingPool();

        int divideIndex = videoData.length / MainManagedBean.THREADS_NO; // 4 threads
        List<Callable<DTWResult>> callables = Arrays.asList(
                new DTWCallable(1, mainManagedBean, delimiterData, videoData, 0, divideIndex),
                new DTWCallable(2, mainManagedBean, delimiterData, videoData, divideIndex, divideIndex * 2),
                new DTWCallable(3, mainManagedBean, delimiterData, videoData, divideIndex * 2, divideIndex * 3),
                new DTWCallable(4, mainManagedBean, delimiterData, videoData, divideIndex * 3, videoData.length)
        );

        ArrayList<DTWResult> results = new ArrayList<>(4);
        try {
            executor.invokeAll(callables)
                    .stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    }).forEach(results::add);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Collections.sort(results, (o1, o2) -> o1.getMinDtw() - o2.getMinDtw());

        return results.get(0);
    }


}
