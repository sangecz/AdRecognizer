package cz.sange.adrecognizer;

import com.sun.istack.internal.NotNull;
import cz.sange.adrecognizer.wavfile.WavFile;
import cz.sange.adrecognizer.wavfile.WavFileException;
import de.crysandt.audio.mpeg7audio.Config;
import de.crysandt.audio.mpeg7audio.ConfigDefault;
import de.crysandt.audio.mpeg7audio.MP7DocumentBuilder;
import org.w3c.dom.Document;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.Part;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;

/**
 * Created by sange on 01/11/15.
 */
@ManagedBean(name = "main")
@SessionScoped
public class MainManagedBean {

    @NotNull
    private Part file;

    private String videoFilePath;
    private String audioFilePath;

    private ResourceBundle resourceBundle;
    private FacesContext ctx;

    public void processFile() {
        uploadFile();
        convertVideoToWav();
        readWav(audioFilePath);

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
            return "Min: "+min+", Max: "+max+", framesRead="+framesReadCnt;
        } catch (Exception e) {
            resourceBundle = ResourceBundle.getBundle("msg");
            FacesMessage m = new FacesMessage(resourceBundle.getString("wavFileErr"));
            ctx.addMessage("form:err", m);
        }
        return in;
    }
}
