package se.herrljunga.astta.utils;

/**
 * A class representing transcribed text along with its associated language.
 */

public class TranscribedCallInformation {


    public TranscribedCallInformation() {
    }

    private String transcribedText;

    private String callDuration;
    private String path;

    public String getPath() {
        return path;
    }

    public TranscribedCallInformation(String transcribedText, String callDuration, String path) {
        this.transcribedText = transcribedText;
        this.callDuration = callDuration;
        this.path = path;
    }

    public String getTranscribedText() {
        return transcribedText;
    }



    public void setTranscribedText(String transcribedText) {
        this.transcribedText = transcribedText;
    }

    public String getCallDuration() {
        return callDuration;
    }

    public void setCallDuration(String callDuration) {
        this.callDuration = callDuration;
    }

    @Override
    public String toString() {
        return getPath();
    }
}
