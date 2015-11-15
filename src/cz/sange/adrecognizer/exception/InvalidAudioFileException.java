package cz.sange.adrecognizer.exception;

/**
 * Created by sange on 12/11/15.
 */
public class InvalidAudioFileException extends Exception {
    public InvalidAudioFileException(String e) {
        super(e);
    }

    public InvalidAudioFileException() {
        super();
    }
}
