package cz.sange.adrecognizer.bean;

import com.sun.istack.internal.NotNull;
import cz.sange.adrecognizer.dtw.DTWResult;
import cz.sange.adrecognizer.dtw.DTWService;
import cz.sange.adrecognizer.model.Delimiter;
import cz.sange.adrecognizer.service.DelimiterService;
import cz.sange.adrecognizer.service.FileManager;
import cz.sange.adrecognizer.srt.SrtFile;
import cz.sange.adrecognizer.srt.SrtRecord;
import cz.sange.adrecognizer.utils.MyUtils;
import org.primefaces.context.RequestContext;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.Part;
import java.io.*;
import java.util.*;

@ManagedBean(name = "main")
@SessionScoped
public class MainManagedBean implements Serializable {

    public static final int THREADS_NO = 4;
    private static final int AD_DURATION_MINS = 5;
    private static final int TH = 1300;
    @NotNull
    private Part file;
    private String srtFilePath;
    private boolean ready;
    private FileManager fileManager;
    // AudioWaveForm data from input video
    private byte [] videoData;
    @EJB
    private DelimiterService delimiterService;

    private ResourceBundle resourceBundle = ResourceBundle.getBundle("msg");

    private String msg = "";
    private String state = "";
    private String progress = "0";
    private String log = "";
    private String logMsg = "";
    private int processed[] = {0, 0, 0, 0};

    ArrayList<DTWResult> results;

    public String processFile() {
//        RequestContext.getCurrentInstance().execute("showLoader();");
        long start = System.currentTimeMillis();
        setState(resourceBundle.getString("working"));
        setReady(false);
        setProgress("0");

        fileManager = FileManager.getInstance();
        fileManager.setFile(file);

        // preparation
        long start2 = System.currentTimeMillis();
        logMsg = "";
        logMsg += "--------------start-------------\n";
        log("upload-start", -1);

        fileManager.uploadFile();

        long end2 = System.currentTimeMillis();
        log("upload-end", (end2 - start2) / 1000);
//        System.out.println("********************** UPLOAD **" + (end2 - start2) / 1000 + " s");
        start2 = System.currentTimeMillis();
        log("convert-start", -1);

        fileManager.convertVideoToWav();

        end2 = System.currentTimeMillis();
        log("convert-end", (end2 - start2) / 1000);
//        System.out.println("********************** CON2WAV **" + (end2 - start2) / 1000 + " s");
        start2 = System.currentTimeMillis();
        log("wave-start", -1);

        // ads detection
        // AudioWaveForm data
        videoData = MyUtils.getWaveFormData(fileManager);

        end2 = System.currentTimeMillis();
        log("wave-end", (end2 - start2) / 1000);
//        System.out.println("********************** WAVEFOR **" + (end2 - start2) / 1000 + " s");
        start2 = System.currentTimeMillis();
        log("match-start", -1);

        tryAllDelimitersFromDB();

        end2 = System.currentTimeMillis();
        log("match-end", (end2 - start2) / 1000);
//        System.out.println("********************** MATCH **" + (end2 - start2) / 1000 + " s");
        start2 = System.currentTimeMillis();
        log("srt-start", -1);

        constructSrtWithAdsDetected();

        end2 = System.currentTimeMillis();
        log("srt-end", (end2 - start2) / 1000);
//        System.out.println("********************** MAKESRT **" + (end2 - start2) / 1000 + " s");
        start2 = System.currentTimeMillis();
        log("cleanup-start", -1);

        fileManager.cleanUp();

        end2 = System.currentTimeMillis();
        log("cleanup-end", (end2 - start2) / 1000);
//        System.out.println("********************** CLEANUP **" + (end2 - start2) / 1000 + " s");

        setReady(true);
        long end = System.currentTimeMillis();
        setState(resourceBundle.getString("done") + " " + (end - start) / 1000 + " s.");
        setProgress("100");
        logMsg += "---------------end--------------\n";
        setLog(logMsg);

//        RequestContext.getCurrentInstance().execute("hideLoader();");
        return "index";
    }

    private void log(String key, long duration) {
        logMsg += resourceBundle.getString(key);
        if (duration != -1){
            logMsg += " " + duration + " s.";
        }
        logMsg += "\n";

        setLog(logMsg);

        // nefunguje
        RequestContext.getCurrentInstance().execute("showProgress();");
    }



    private void tryAllDelimitersFromDB() {
//        System.out.println("*********VIDEO: #bytes=" + videoData.length);

        DTWService dtwService = new DTWService(videoData, this);
        results = new ArrayList<>(THREADS_NO);
        List<Delimiter> delimiterList = delimiterService.getAll();
        for (Delimiter d : delimiterList) {

//            System.out.println("*********DELIMITER: #bytes=" + d.getData().length + ", name=" + d.getName());
            // DTW
            results.addAll(dtwService.start(d.getData()));

//            msg = "";
//            for (DTWResult result : results) {
//                msg += "DTWdist=" + result.getMinDtw() + ", pos="
//                        + MyUtils.getFormattedTime(
//                        MyUtils.getAudioFileDuration(fileManager.getAudioFilePath()) * (result.getPosition() + 0.0) / videoData.length)
//                        + ", dur=" + MyUtils.getFormattedTime(MyUtils.getAudioFileDuration(fileManager.getAudioFilePath())) + "== ";
//            }
        }

        // remove results > TH
        ArrayList<DTWResult> toRemove = new ArrayList<>(results.size());
        for (DTWResult r : results) {
            if(r.getMinDtw() > TH) {
                toRemove.add(r);
            }
        }
        toRemove.forEach(results::remove);
        toRemove = null;

        // sort by distance
        Collections.sort(results, (o1, o2) -> o1.getMinDtw() - o2.getMinDtw());
    }

    private void constructSrtWithAdsDetected() {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
        srtFilePath = fileManager.getVideoFilePath() + ".srt";
        SrtFile srtFile = new SrtFile(srtFilePath);

        int videoMins = (int) (MyUtils.getAudioFileDuration(fileManager.getAudioFilePath()) / 60);
        // assumption, ads max every 10mins
        int th = Math.ceil(videoMins / 10) > results.size() ? results.size() : (int) Math.ceil(videoMins / 10);
        // get sublist
        th = th == 0 ? 1 : th;
        if (th < results.size()) {
            int size = results.size();
            for (int i = size - 1; i >= th; i--) {
                results.remove(i);
            }
        }
        // sort again, this time by position in video
        Collections.sort(results, (o1, o2) -> o1.getPosition() - o2.getPosition());

        for (int i = 0; i < th; i++) {
            DTWResult result = results.get(i);

            double startTime = MyUtils.getAudioFileDuration(fileManager.getAudioFilePath()) * (result.getPosition() + 0.0) / videoData.length;
            String start = MyUtils.getFormattedTime(startTime);

            double durationTime = (startTime + AD_DURATION_MINS * 60); // add 5min
            String end = MyUtils.getFormattedTime(durationTime);

            SrtRecord r = new SrtRecord(i + 1, start + " --> " + end, resourceBundle.getString("advertismentStr"));
            srtFile.appendSrtRecord(r);
        }

        srtFile.write();
    }

    public Part getFile() {
        return file;
    }

    public void setFile(Part file) {
        this.file = file;
    }

//    private String readWav(String in) {
//        try {
//            // Open the wav file specified as the first argument
//            WavFile wavFile = WavFile.openWavFile(new File(in));
//
//            // Display information about the wav file
//            wavFile.display();
//
////            System.out.println("LEN:::::: " + wavFile.getNumFrames() / wavFile.getSampleRate());
//
//            // Get the number of audio channels in the wav file
//            int numChannels = wavFile.getNumChannels();
//
//            // Create a buffer of 100 frames
//            double[] buffer = new double[100 * numChannels];
//
//            int framesRead;
//            double min = Double.MAX_VALUE;
//            double max = Double.MIN_VALUE;
//
//            long framesReadCnt = 0;
//            do {
//                // Read frames into buffer
//                framesRead = wavFile.readFrames(buffer, 100);
//                framesReadCnt += framesRead;
//
//                // Loop through frames and look for minimum and maximum value
//                for (int s=0 ; s<framesRead * numChannels ; s++)
//                {
//                    if (buffer[s] > max) max = buffer[s];
//                    if (buffer[s] < min) min = buffer[s];
//                }
//            }
//            while (framesRead != 0);
//
//            // Close the wavFile
//            wavFile.close();
//
//            // Output the minimum and maximum value
//            System.out.println("Min: "+min+", Max: "+max+", framesRead="+framesReadCnt);
//            // TODO
//        } catch (Exception e) {
//            ResourceBundle resourceBundle = ResourceBundle.getBundle("msg");
//            FacesMessage m = new FacesMessage(resourceBundle.getString("wavFileErr") + " " + e.toString());
//            FacesContext ctx = FacesContext.getCurrentInstance();
//            ctx.addMessage("form:err", m);
//        }
//        return in;
//    }

    public void download() throws IOException {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();

        ec.responseReset(); // Some JSF component library or some Filter might have set some headers in the buffer beforehand. We want to get rid of them, else it may collide.
//        ec.setResponseContentType(contentType); // Check http://www.iana.org/assignments/media-types for all types. Use if necessary ExternalContext#getMimeType() for auto-detection based on filename.
//        ec.setResponseContentLength(contentLength); // Set it with the file size. This header is optional. It will work if it's omitted, but the download progress will be unknown.
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + srtFilePath + "\""); // The Save As popup magic is done here. You can give it any file name you want, this only won't work in MSIE, it will use current request URL as file name instead.

        OutputStream output = ec.getResponseOutputStream();
        // Now you can write the InputStream of the file to the above OutputStream the usual way.
        InputStream is = new FileInputStream(new File(srtFilePath));
        byte[] bytesBuffer = new byte[2048];
        int bytesRead;
        while ((bytesRead = is.read(bytesBuffer)) > 0) {
            output.write(bytesBuffer, 0, bytesRead);
        }

        // Make sure that everything is out
        output.flush();

        // Close both streams
        is.close();
        output.close();

        fc.responseComplete(); // Important! Otherwise JSF will attempt to render the response which obviously will fail since it's already written with a file and closed.

//        fileManager.deleteTemporaryFile(srtFilePath);
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public void setProcessed(int processed, int from) {
        this.processed[from - 1] = processed;

        int cnt = 0;
        for (int i = 0; i < THREADS_NO; i++) {
            cnt += this.processed[i];
        }

        setProgress(cnt * 100 / videoData.length / delimiterService.getAll().size() + "");
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }
}
