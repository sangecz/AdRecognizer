package cz.sange.adrecognizer.utils;

import cz.sange.adrecognizer.service.FileManager;
import cz.sange.adrecognizer.wavhelpers.WavFile;
import de.crysandt.audio.mpeg7audio.Config;
import de.crysandt.audio.mpeg7audio.ConfigDefault;
import de.crysandt.audio.mpeg7audio.MP7DocumentBuilder;
import org.w3c.dom.Document;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ResourceBundle;

/**
 * Created by sange on 05/01/16.
 */
public class MyUtils {

    public static byte[] getWaveFormData(FileManager fileManager) {
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
        // set HopSize in ms
        config.setValue("Resizer", "HopSize", Integer.parseInt(ResourceBundle.getBundle("config").getString("hopSize")));
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

    public static double getAudioFileDuration(String in) {
        double duration = -1;
        try {
            // Open the wav file specified as the first argument
            WavFile wavFile = WavFile.openWavFile(new File(in));

            // Display information about the wav file
            duration = wavFile.getDuration();
            // Close the wavFile
            wavFile.close();

        } catch (Exception e) {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("msg");
            FacesMessage m = new FacesMessage(resourceBundle.getString("wavFileErr") + " " + e.toString());
            FacesContext ctx = FacesContext.getCurrentInstance();
            ctx.addMessage("form:err", m);
        }
        return duration; // in seconds
    }

    // time in secs
    public static String getFormattedTime(double time) {
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

        return hoursStr + ":" + minsStr + ":" + secsStr + "," + msecsStr;
    }

}
