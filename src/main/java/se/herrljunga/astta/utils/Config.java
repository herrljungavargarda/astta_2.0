package se.herrljunga.astta.utils;

import java.util.List;

public class Config {
    public static class SpeechToText {
        public String region;
        public List<String> supportedLanguages;
        public String secretName;
    }

    public static class BlobStorage {
        public String accountSecretName;
        public String accountSecretKey;
        public String audioSourceContainerName;
        public String reportSaveContainerName;
        public String powerBiContainerName;
        public String tempContainerName;
        public String sasTokenSecretName;
        public String endpoint;
    }

    public static class OpenAI {
        public String secretName;
        public String endpoint;
        public String model;
        public String promptPath;
    }

    public static class Utils {
        public String pathToTemp;
        public String analyzedJsonSaveDirectory;
        public String transcribedTextSaveDirectory;
        public String sasTokenTranscriptionBlobSecretName;
        public String transcriptionApiUrl;
        public String transcriptionsApiUrlPreview;
    }

    public SpeechToText speechToText;
    public BlobStorage blobStorage;
    public OpenAI openAI;
    public Utils utils;
    public int maxThreadsForAnalysis;
}