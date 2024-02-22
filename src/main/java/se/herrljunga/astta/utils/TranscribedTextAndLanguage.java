package se.herrljunga.astta.utils;

public class TranscribedTextAndLanguage {
    private String transcribedText;
    private String language;

    public TranscribedTextAndLanguage(String transcribedText, String language) {
        this.transcribedText = transcribedText;
        this.language = language;
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
}
