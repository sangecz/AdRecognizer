package cz.sange.adrecognizer.service;

import cz.sange.adrecognizer.utils.MyUtils;
import cz.sange.adrecognizer.wavhelpers.SeparateAudioVideo;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.Part;
import java.io.*;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class FileManager {

    private String videoFilePath;
    private String audioFilePath;
    private Part file;
    private static FileManager instance;

    public static FileManager getInstance(){
        if (instance == null) {
            instance = new FileManager();
        }
        return instance;
    }

    private FileManager() {
    }

    public void convertVideoToWav() {
        SeparateAudioVideo separateAudioVideo =
                new SeparateAudioVideo();
        separateAudioVideo.process(videoFilePath, audioFilePath);
    }

    public void uploadFile() {
        // Create path components to save the file
        ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
        FacesContext ctx = FacesContext.getCurrentInstance();

        final String path = resourceBundle.getString("videoFileDestination");
        final Part filePart = file;
        final String fileName = MyUtils.getFileName(filePart);
        videoFilePath = path + File.separator + fileName;
        audioFilePath = path + File.separator + fileName + (!file.getContentType().startsWith("audio/wav") ? ".wav" : "");

        OutputStream out = null;
        InputStream filecontent = null;

        try {
            if (!file.getContentType().startsWith("audio/wav")) {
                out = new FileOutputStream(new File(videoFilePath));
            } else {
                out = new FileOutputStream(new File(audioFilePath));
            }
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

    public void cleanUp(){
        // cleanup
        deleteTemporaryFile(videoFilePath);
        deleteTemporaryFile(audioFilePath);
    }

    public void deleteTemporaryFile(String filePath) {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("msg");
        try {
            File file = new File(filePath);
            if(!file.delete()){
                String s = resourceBundle.getString("deleteTmpErr");
                String msg = MessageFormat.format(s, filePath);
                FacesContext.getCurrentInstance().addMessage("form:err", new FacesMessage(msg));
            }
        } catch(Exception e) {
            FacesMessage m = new FacesMessage(resourceBundle.getString("ioException"));
            FacesContext.getCurrentInstance().addMessage("form:err", m);
        }
    }

    public String getVideoFilePath() {
        return videoFilePath;
    }

    public String getAudioFilePath() {
        return audioFilePath;
    }

    public void setFile(Part file) {
        this.file = file;
    }
}
