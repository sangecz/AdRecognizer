package cz.sange.adrecognizer.bean;

import com.sun.istack.internal.NotNull;
import cz.sange.adrecognizer.model.Delimiter;
import cz.sange.adrecognizer.service.DelimiterService;
import cz.sange.adrecognizer.service.FileManager;
import cz.sange.adrecognizer.srt.SrtFile;
import cz.sange.adrecognizer.srt.SrtRecord;
import cz.sange.adrecognizer.wavfile.WavFile;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
//import javax.enterprise.context.SessionScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
//import javax.inject.Named;
import javax.servlet.http.Part;
import java.io.*;
import java.util.*;

/** TODO
 *  * vytvorit DB reklamnich predelu
 *      - zkontrolovat zaznam predelu v DB
 *  * detekovat reklamy na zaklade DB predelu
 *      - pro kazdy zaznam v DB predelu: posouvat okenko zaznamu po WAVu a zapisovat do SRT identifikovana mista
 *      - pouzit DWT: dynamic time warping distance !! ...
 *      - na obr. ukazka hledani v tabulce: rekl x film (velikost okenko) + optimalizace - zahazovat deviace
 */

@ManagedBean(name = "main")
@SessionScoped
public class MainManagedBean implements Serializable {

    @NotNull
    private Part file;
    private String srtFilePath;
    private boolean ready;
    private FileManager fileManager;
    @EJB
    private DelimiterService delimiterService;


    public void processFile() {
        fileManager = FileManager.getInstance();
        fileManager.setFile(file);

        // preparation
        fileManager.uploadFile();
        fileManager.convertVideoToWav();

        // ads detection
        tryAllDelimitersFromDB();
        constructSrtWithAdsDetected();

//        fileManager.cleanUp();
    }

    private void tryAllDelimitersFromDB() {

        List<Delimiter> delimiterList = delimiterService.getAll();

        FileOutputStream fos = null;
        int idx = 0;
        for (Delimiter d : delimiterList) {
            // 1) get delimiter WAV
            String filePath = "/tmp/__test" + (idx++) + ".wav";
            try {
                fos = new FileOutputStream(filePath);
                fos.write(d.getData());
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 2) try to 'find' it in provided WAV
            // TODO detect ad
            readWav(fileManager.getAudioFilePath());

            // 3) delete delimiter WAV
            fileManager.deleteTemporaryFile(filePath);
        }

    }

    private void constructSrtWithAdsDetected() {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
        srtFilePath = fileManager.getVideoFilePath() + ".srt";
        SrtFile srtFile = new SrtFile(srtFilePath);

        SrtRecord r = new SrtRecord(1, "00:00:10,000 --> 00:00:20,000", resourceBundle.getString("advertismentStr"));
        srtFile.appendSrtRecord(r);

        srtFile.write();
        ready = true;
    }

    public Part getFile() {
        return file;
    }

    public void setFile(Part file) {
        this.file = file;
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

            StringBuilder sb = new StringBuilder(100);
            int rounds = 0;
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
                    sb.append(buffer[s]);
                    sb.append(" ");
                }

//                setProgress((int)(100 * framesReadCnt / totalNumFrames));
//                if(rounds < 100) {
//                    System.out.println(rounds++ + "# " + sb.toString());
//                }
                sb.setLength(0);
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

}
