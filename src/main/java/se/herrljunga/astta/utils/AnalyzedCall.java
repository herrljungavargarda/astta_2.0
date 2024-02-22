package se.herrljunga.astta.utils;

public class AnalyzedCall {
    private final String savePath;
    private final String analyzedCallJson;

    public AnalyzedCall(String savePath, String analyzedCallJson) {
        this.savePath = savePath;
        this.analyzedCallJson = analyzedCallJson;
    }

    public String getSavePath() {
        return savePath;
    }

    public String getAnalyzedCallJson() {
        return analyzedCallJson;
    }
}
