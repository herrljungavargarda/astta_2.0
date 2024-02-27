package se.herrljunga.astta.analyze;

public interface Analyze {
    String removeSensitiveInformation(String text);
    String getSummarize(String text);
    String getContext(String text);
    String getScore(String text);
}
