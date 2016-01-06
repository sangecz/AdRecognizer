package cz.sange.adrecognizer.bean;

import cz.sange.adrecognizer.service.FileManager;
import cz.sange.adrecognizer.model.Delimiter;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.Part;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.ResourceBundle;

import cz.sange.adrecognizer.utils.MyUtils;
import de.crysandt.audio.mpeg7audio.*;
import org.w3c.dom.Document;


@ManagedBean
@SessionScoped
public class DelimiterManagedBean {

    private long length;
    private String name;
    private Part file;
    private byte [] data;
    @ManagedProperty("#{database}")
    private DatabaseManagedBean database;

    public DelimiterManagedBean() {
    }

    public String upload() {
        Delimiter d = new Delimiter();
        d.setName(this.name);

        FileManager fileManager = FileManager.getInstance();
        fileManager.setFile(file);
        fileManager.uploadFile();
        fileManager.convertVideoToWav();

//        try {
//            RandomAccessFile f = new RandomAccessFile(fileManager.getAudioFilePath(), "r");
//            data = new byte[(int)f.length()];
//            f.read(data);
//        } catch (IOException e) {
//            ResourceBundle resourceBundle = ResourceBundle.getBundle("msg");
//            FacesMessage m = new FacesMessage(resourceBundle.getString("ioException") + ": " + e.toString());
//            FacesContext ctx = FacesContext.getCurrentInstance();
//            ctx.addMessage("form:err", m);
//        }

        data = MyUtils.getWaveFormData(fileManager);
        d.setData(data);
        database.addDelimiter(d);

        fileManager.cleanUp();
        return "delimiters";
    }

    public String delete(long id){
        database.deleteDelimiter(id);
        return "delimiters";
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Part getFile() {
        return file;
    }

    public void setFile(Part file) {
        this.file = file;
    }

    public long getLength() {
        return length;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setDatabase(DatabaseManagedBean database) {
        this.database = database;
    }

//    public byte[] getWaveFormData(FileManager fileManager) {
//        AudioInputStream ais = null;
//        try {
//            ais = AudioSystem.getAudioInputStream(new File(fileManager.getAudioFilePath()));
//        } catch (UnsupportedAudioFileException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Config config = new ConfigDefault();
//
//        /* Turn all descriptors off */
//        config.enableAll(false);
//        // set HopSize in ms
//        config.setValue("Resizer", "HopSize", Integer.parseInt(ResourceBundle.getBundle("config").getString("hopSize")));
//        config.setValue("AudioWaveform", "enable", true); // Extract the AudioWaveform
//
//        Document mpeg7;
//        try {
//            assert ais != null;
//            mpeg7 = MP7DocumentBuilder.encode(ais, config);
//
//            String [] min = mpeg7.getElementsByTagName("Min").item(0).getTextContent().split(" ");
//
//
//            // 'toBytes'
//            int count = min.length;
////            System.out.println("COUNT_IN=" + count);
//            byte [][] minBytes = new byte[count][];
//
//            for (int i = 0; i < count; i++) {
//                minBytes[i] = min[i].getBytes(Charset.forName("UTF-8"));
//            }
////            System.out.println("MIN_IN: " + Arrays.deepToString(min));
//            min = null;
//
//            byte [][] maxBytes = new byte[count][];
//            String [] max = mpeg7.getElementsByTagName("Max").item(0).getTextContent().split(" ");
//            for (int i = 0; i < count; i++) {
//                maxBytes[i] = max[i].getBytes(Charset.forName("UTF-8"));
//            }
////            System.out.println("MAX_IN: " + Arrays.deepToString(max));
//            max = null;
//
//            byte [][] minMax = new byte[count * 2][];
//            System.arraycopy(minBytes, 0, minMax, 0, count);
//            System.arraycopy(maxBytes, 0, minMax, count, count);
//
//
//            ByteArrayOutputStream bucket = new ByteArrayOutputStream();
//            DataOutputStream out = new DataOutputStream(bucket);
//
//            for(byte[] row : minMax) {
//                for(int i : row) {
//                    try {
//                        out.write(i);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//            return bucket.toByteArray();
////            System.out.println("MIN_BYTES: " + Arrays.deepToString(minBytes));
////            System.out.println("DATAES: " + Arrays.toString(data));
//
////            // TESTING 'toString'
////            int countOut = minMax.length / 2;
////            System.out.println("COUNT_OUT=" + countOut);
////
////            String [] minOut = new String[countOut];
////            String [] maxOut = new String[countOut];
////            byte [][] minBytesOut = new byte[countOut][];
////            byte [][] maxBytesOut = new byte[countOut][];
////
////            System.arraycopy(minMax, 0, minBytesOut, 0, countOut);
////            System.arraycopy(minMax, countOut, maxBytesOut, 0, countOut);
////
////            for (int i = 0; i < countOut; i++) {
////                minOut[i] = new String(minBytesOut[i], Charset.forName("UTF-8"));
////                maxOut[i] = new String(maxBytesOut[i], Charset.forName("UTF-8"));
////            }
////            System.out.println("MIN_OUT" + Arrays.toString(minOut));
////            System.out.println("MAX_OUT" + Arrays.toString(maxOut));
//
//        } catch (ParserConfigurationException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
}
