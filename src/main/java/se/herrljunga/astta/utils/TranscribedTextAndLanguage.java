package se.herrljunga.astta.utils;

/**
 * A class representing transcribed text along with its associated language.
 */

public class TranscribedTextAndLanguage {


    public TranscribedTextAndLanguage() {
    }

    private String transcribedText;

    private String language;
    private String path;

    public String getPath() {
        return path;
    }

    public TranscribedTextAndLanguage(String transcribedText, String language, String path) {
        this.transcribedText = transcribedText;
        this.language = language;
        this.path = path;
    }

    public String getTranscribedText() {
        return transcribedText;
    }

    public String getLanguage() {
        return language;
    }

    public void setTranscribedText(String transcribedText) {
        this.transcribedText = transcribedText;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return getPath();
    }
}
