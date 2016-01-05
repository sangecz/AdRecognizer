package cz.sange.adrecognizer.bean;

import com.sun.istack.internal.NotNull;
import cz.sange.adrecognizer.dtw.DTWResult;
import cz.sange.adrecognizer.dtw.DTWService;
import cz.sange.adrecognizer.model.Delimiter;
import cz.sange.adrecognizer.service.DelimiterService;
import cz.sange.adrecognizer.service.FileManager;
import cz.sange.adrecognizer.srt.SrtFile;
import cz.sange.adrecognizer.srt.SrtRecord;
import cz.sange.adrecognizer.wavhelpers.WavFile;
import de.crysandt.audio.mpeg7audio.Config;
import de.crysandt.audio.mpeg7audio.ConfigDefault;
import de.crysandt.audio.mpeg7audio.MP7DocumentBuilder;
import org.primefaces.context.RequestContext;
import org.w3c.dom.Document;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
//import javax.enterprise.context.SessionScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
//import javax.inject.Named;
import javax.servlet.http.Part;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/** TODO
 *  ## funkcni prevod:
 *  # vystrizeni vydea
 *  $ ffmpeg -ss 00:28:26.800 -i input.mpg -t 00:00:05.000 -c:v copy -c:a copy out-cut.mpg
 *  # prevod na akceptovatelny format Javou
 *  $ ffmpeg -i out-cut.mpg -vcodec libx264 -preset slow -crf 22 -threads 0 -acodec libfaac -ar 44100 rekl-predel.mp4
 *
 *  * detekovat reklamy na zaklade DB predelu
 *      - pro kazdy zaznam v DB predelu: posouvat okenko zaznamu po WAVu a zapisovat do SRT identifikovana mista
 *      - pouzit DTW: dynamic time warping distance !! ...
 */

@ManagedBean(name = "main")
@SessionScoped
public class MainManagedBean implements Serializable {

    public static final int THREADS_NO = 4;
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

    public String processFile() {
//        RequestContext.getCurrentInstance().execute("showLoader();");
        long start = System.currentTimeMillis();
        setState(resourceBundle.getString("working"));
        setReady(false);

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
        System.out.println("********************** UPLOAD **" + (end2 - start2) / 1000 + " s");
        start2 = System.currentTimeMillis();
        log("convert-start", -1);

        fileManager.convertVideoToWav();

        end2 = System.currentTimeMillis();
        log("convert-end", (end2 - start2) / 1000);
        System.out.println("********************** CON2WAV **" + (end2 - start2) / 1000 + " s");
        start2 = System.currentTimeMillis();
        log("wave-start", -1);

        // ads detection
        // AudioWaveForm data
        videoData = getWaveFormData(fileManager);

        end2 = System.currentTimeMillis();
        log("wave-end", (end2 - start2) / 1000);
        System.out.println("********************** WAVEFOR **" + (end2 - start2) / 1000 + " s");
        start2 = System.currentTimeMillis();
        log("match-start", -1);

        tryAllDelimitersFromDB();

        end2 = System.currentTimeMillis();
        log("match-end", (end2 - start2) / 1000);
        System.out.println("********************** MATCH **" + (end2 - start2) / 1000 + " s");
        start2 = System.currentTimeMillis();
        log("srt-start", -1);

        constructSrtWithAdsDetected();

        end2 = System.currentTimeMillis();
        log("srt-end", (end2 - start2) / 1000);
        System.out.println("********************** MAKESRT **" + (end2 - start2) / 1000 + " s");
        start2 = System.currentTimeMillis();
        log("cleanup-start", -1);

        fileManager.cleanUp();

        end2 = System.currentTimeMillis();
        log("cleanup-end", (end2 - start2) / 1000);
        System.out.println("********************** CLEANUP **" + (end2 - start2) / 1000 + " s");

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

        RequestContext.getCurrentInstance().execute("showProgress();");
    }

    public byte[] getWaveFormData(FileManager fileManager) {
        // FIXME prendat nekam do tools, pristupne i pro MainManagedBean

        AudioInputStream ais = null;
        try {
            ais = AudioSystem.getAudioInputStream(new File(fileManager.getAudioFilePath()));
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Config config = new ConfigDefault();

        /* Turn all descriptors off */
        config.enableAll(false);

        config.setValue("Resizer", "HopSize", 100);        // set HopSize in ms
        config.setValue("AudioWaveform", "enable", true); // Extract the AudioWaveform

        Document mpeg7;
        try {
            assert ais != null;
            mpeg7 = MP7DocumentBuilder.encode(ais, config);

            String [] min = mpeg7.getElementsByTagName("Min").item(0).getTextContent().split(" ");

            // 'toBytes'
            int count = min.length;
//            System.out.println("COUNT_IN=" + count);
            byte [][] minBytes = new byte[count][];

            for (int i = 0; i < count; i++) {
                minBytes[i] = min[i].getBytes(Charset.forName("UTF-8"));
            }
//            System.out.println("MIN_IN: " + Arrays.deepToString(min));
            min = null;

            byte [][] maxBytes = new byte[count][];
            String [] max = mpeg7.getElementsByTagName("Max").item(0).getTextContent().split(" ");
            for (int i = 0; i < count; i++) {
                maxBytes[i] = max[i].getBytes(Charset.forName("UTF-8"));
            }
//            System.out.println("MAX_IN: " + Arrays.deepToString(max));
            max = null;

            byte [][] minMax = new byte[count * 2][];
            System.arraycopy(minBytes, 0, minMax, 0, count);
            System.arraycopy(maxBytes, 0, minMax, count, count);


            ByteArrayOutputStream bucket = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bucket);

            for(byte[] row : minMax) {
                for(int i : row) {
                    try {
                        out.write(i);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return bucket.toByteArray();
//            System.out.println("MIN_BYTES: " + Arrays.deepToString(minBytes));
//            System.out.println("DATAES: " + Arrays.toString(data));

//            // TESTING 'toString'
//            int countOut = minMax.length / 2;
//            System.out.println("COUNT_OUT=" + countOut);
//
//            String [] minOut = new String[countOut];
//            String [] maxOut = new String[countOut];
//            byte [][] minBytesOut = new byte[countOut][];
//            byte [][] maxBytesOut = new byte[countOut][];
//
//            System.arraycopy(minMax, 0, minBytesOut, 0, countOut);
//            System.arraycopy(minMax, countOut, maxBytesOut, 0, countOut);
//
//            for (int i = 0; i < countOut; i++) {
//                minOut[i] = new String(minBytesOut[i], Charset.forName("UTF-8"));
//                maxOut[i] = new String(maxBytesOut[i], Charset.forName("UTF-8"));
//            }
//            System.out.println("MIN_OUT" + Arrays.toString(minOut));
//            System.out.println("MAX_OUT" + Arrays.toString(maxOut));

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void tryAllDelimitersFromDB() {
        System.out.println("VIDEO: #bytes=" + videoData.length);

        DTWService dtwService = new DTWService(videoData, this);
        DTWResult result;
        List<Delimiter> delimiterList = delimiterService.getAll();
        for (Delimiter d : delimiterList) {

            System.out.println("DELIMITER: #bytes=" + d.getData().length + ", name=" + d.getName());

            // DTW
            result = dtwService.start(d.getData());

            msg = "DTWdist=" + result.getMinDtw() + ", pos="
                    + getFormattedTime(
                    getAudioFileDuration(fileManager.getAudioFilePath()) * (result.getPosition() + 0.0) / videoData.length)
                    + ", dur=" + getFormattedTime(getAudioFileDuration(fileManager.getAudioFilePath()));
        }
    }

    private void constructSrtWithAdsDetected() {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
        srtFilePath = fileManager.getVideoFilePath() + ".srt";
        SrtFile srtFile = new SrtFile(srtFilePath);

        SrtRecord r = new SrtRecord(1, "00:00:10,000 --> 00:00:20,000", resourceBundle.getString("advertismentStr"));
        srtFile.appendSrtRecord(r);

        srtFile.write();
    }

    // time in secs
    private String getFormattedTime(double time) {
        int hours = (int) (time / 3600);
        int mins = (int) ((time - hours * 3600) / 60);
        int secs = (int) (time - hours * 3600 - mins * 60);
        int msecs = (int) ((time - hours * 3600 - mins * 60 - secs) / 1000);

        String hoursStr = Integer.toString(hours);
        hoursStr = (hoursStr.startsWith("0") || hoursStr.length() == 1 ? "0" + hoursStr : hoursStr);
        String minsStr = Integer.toString(mins);
        minsStr = (minsStr.startsWith("0") || minsStr.length() == 1 ? "0" + minsStr : minsStr);
        String secsStr = Integer.toString(secs);
        secsStr = (secsStr.startsWith("0") || secsStr.length() == 1 ? "0" + secsStr : secsStr);
        String msecsStr = Integer.toString(msecs);
        if("0".equals(msecsStr)) {
            msecsStr = "000";
        } else {
            if(msecsStr.length() == 2) {
                msecsStr = "0" + msecsStr;
            }
            if (msecsStr.length() == 1) {
                msecsStr = "00" + msecsStr;
            }
        }

        return hoursStr + ":" + minsStr + ":" + secsStr + "." + msecsStr;
    }

    public Part getFile() {
        return file;
    }

    public void setFile(Part file) {
        this.file = file;
    }

    private double getAudioFileDuration(String in) {
        double duration = -1;
        try {
            // Open the wav file specified as the first argument
            WavFile wavFile = WavFile.openWavFile(new File(in));

            // Display information about the wav file
            duration = wavFile.getDuration();

//
            // Close the wavFile
            wavFile.close();

        } catch (Exception e) {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("msg");
            FacesMessage m = new FacesMessage(resourceBundle.getString("wavFileErr") + " " + e.toString());
            FacesContext ctx = FacesContext.getCurrentInstance();
            ctx.addMessage("form:err", m);
        }
        return duration;
    }

    private String readWav(String in) {
        try {
            // Open the wav file specified as the first argument
            WavFile wavFile = WavFile.openWavFile(new File(in));

            // Display information about the wav file
            wavFile.display();

//            System.out.println("LEN:::::: " + wavFile.getNumFrames() / wavFile.getSampleRate());

            // Get the number of audio channels in the wav file
            int numChannels = wavFile.getNumChannels();

            // Create a buffer of 100 frames
            double[] buffer = new double[100 * numChannels];

            int framesRead;
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;

            long framesReadCnt = 0;
            do {
                // Read frames into buffer
                framesRead = wavFile.readFrames(buffer, 100);
                framesReadCnt += framesRead;

                // Loop through frames and look for minimum and maximum value
                for (int s=0 ; s<framesRead * numChannels ; s++)
                {
                    if (buffer[s] > max) max = buffer[s];
                    if (buffer[s] < min) min = buffer[s];
                }
            }
            while (framesRead != 0);

            // Close the wavFile
            wavFile.close();

            // Output the minimum and maximum value
            System.out.println("Min: "+min+", Max: "+max+", framesRead="+framesReadCnt);
            // TODO
        } catch (Exception e) {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("msg");
            FacesMessage m = new FacesMessage(resourceBundle.getString("wavFileErr") + " " + e.toString());
            FacesContext ctx = FacesContext.getCurrentInstance();
            ctx.addMessage("form:err", m);
        }
        return in;
    }

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

        fileManager.deleteTemporaryFile(srtFilePath);
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

        setProgress(cnt * 100 / videoData.length + "");
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }
}
