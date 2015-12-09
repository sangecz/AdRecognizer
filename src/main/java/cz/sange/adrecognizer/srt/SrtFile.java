package cz.sange.adrecognizer.srt;

import java.io.*;

/**
 * Created by sange on 16/11/15.
 */
public class SrtFile {

    private FileOutputStream outputStream;
    private StringBuilder stringBuilder;
    private String filePath;
    private final String ENCODING = "UTF-8";

    public SrtFile(String filePath) {
        this.filePath = filePath;
        stringBuilder = new StringBuilder();
    }

    public void appendSrtRecord(SrtRecord record) {
        stringBuilder.append(record.toString());
    }

    public void write() {
        Writer out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(filePath), ENCODING));

            out.write(stringBuilder.toString());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

}
