package cz.sange.adrecognizer;

import com.sun.istack.internal.NotNull;
import cz.sange.adrecognizer.srt.SrtFile;
import cz.sange.adrecognizer.srt.SrtRecord;
import cz.sange.adrecognizer.wavfile.SeparateAudioVideo;
import cz.sange.adrecognizer.wavfile.WavFile;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.Part;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;

/** TODO
 *  * vytvorit DB reklamnich predelu
 *      - JPM - postgresql
 *      - navrhnout DB zaznamy predelu
 *      - do UI uziv. pridavani rekl. predelu - IN: video predel, OUT: zaznam predelu v DB
 *  * detekovat reklamy na zaklade DB predelu
 *      - pro kazdy zaznam v DB predelu: posouvat okenko zaznamu po WAVu a zapisovat do SRT identifikovana mista
 *  * loading UI
 */

@ManagedBean(name = "main")
@SessionScoped
public class MainManagedBean {

    @NotNull
    private Part file;

    private String videoFilePath;
    private String audioFilePath;
    private String srtFilePath;
    private boolean ready;
    private int progress;

    private ResourceBundle resourceBundle;
    private FacesContext ctx;

    public void processFile() {
        // preparation
        progress = 0;
        uploadFile();
        convertVideoToWav();

        // ads detection
        // TODO detect ad
        readWav(audioFilePath);
        constructSrtWithAdsDetected();

        // cleanup
        deleteTemporaryFile(videoFilePath);
        deleteTemporaryFile(audioFilePath);
        progress = 100;
//        deleteTemporaryFile(srtFilePath);
    }

    private void deleteTemporaryFile(String filePath) {
        resourceBundle = ResourceBundle.getBundle("msg");
        try {
            File file = new File(filePath);
            if(!file.delete()){
                String s = resourceBundle.getString("deleteTmpErr");
                String msg = MessageFormat.format(s, filePath);
                ctx.addMessage("form:err", new FacesMessage(msg));
            }
        } catch(Exception e) {
            FacesMessage m = new FacesMessage(resourceBundle.getString("ioException"));
            ctx.addMessage("form:err", m);
        }
    }

    private void constructSrtWithAdsDetected() {
        resourceBundle = ResourceBundle.getBundle("config");
        srtFilePath = videoFilePath + ".srt";
        SrtFile srtFile = new SrtFile(srtFilePath);

        SrtRecord r = new SrtRecord(1, "00:00:10,000 --> 00:00:20,000", resourceBundle.getString("advertismentStr"));
        srtFile.appendSrtRecord(r);

        srtFile.write();
        ready = true;
    }

    private void convertVideoToWav() {
        SeparateAudioVideo separateAudioVideo =
                new SeparateAudioVideo();
        separateAudioVideo.process(videoFilePath, audioFilePath);
    }

    private void uploadFile() {
        // Create path components to save the file
        resourceBundle = ResourceBundle.getBundle("config");
        ctx = FacesContext.getCurrentInstance();

        final String path = resourceBundle.getString("videoFileDestination");
        final Part filePart = file;
        final String fileName = getFileName(filePart);
        videoFilePath = path + File.separator + fileName;
        audioFilePath = path + File.separator + fileName + ".wav";

        OutputStream out = null;
        InputStream filecontent = null;

        try {
            out = new FileOutputStream(new File(videoFilePath));
            filecontent = filePart.getInputStream();

            int read = 0;
            final byte[] bytes = new byte[1024];

            while ((read = filecontent.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
        } catch (IOException e) {
            FacesMessage m = new FacesMessage(resourceBundle.getString("fileNotFound"));
            ctx.addMessage("form:err", m);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (filecontent != null) {
                    filecontent.close();
                }
            } catch (IOException e){
                FacesMessage m = new FacesMessage(resourceBundle.getString("ioException"));
                ctx.addMessage("form:err", m);
            }
        }
    }

    private String getFileName(final Part part) {
        final String partHeader = part.getHeader("content-disposition");
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(
                        content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
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
            resourceBundle = ResourceBundle.getBundle("msg");
            FacesMessage m = new FacesMessage(resourceBundle.getString("wavFileErr"));
            ctx.addMessage("form:err", m);
        }
        return in;
    }

    public String getSrtFilePath() {
        return srtFilePath;
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

        deleteTemporaryFile(srtFilePath);
    }

    public boolean isReady() {
        return ready;
    }

    public int getProgress() {
        return progress;
    }
}
