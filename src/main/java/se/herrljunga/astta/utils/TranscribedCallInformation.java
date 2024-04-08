package se.herrljunga.astta.utils;


/**
 * The TranscribedCallInformation class.
 *
 * This class represents transcribed text along with its associated call duration and path.
 * It includes getter and setter methods for the transcribed text, call duration, and path.
 * It also overrides the toString method to return the path.
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
