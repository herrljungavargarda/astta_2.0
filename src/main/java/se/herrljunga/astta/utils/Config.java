package se.herrljunga.astta.utils;

import java.util.List;

public class Config {
    //Speech To Text
    public static String speechToTextRegion = "swedencentral"; // Location of your region host (can be found when signed in to Azure Portal under Speech service)
    public static List<String> supportedLanguages = List.of("sv-SE", "en-US"); // Languages the AI supports (first is base language), for all supported languages refer to this page "https://learn.microsoft.com/en-us/azure/ai-services/speech-service/language-support?tabs=stt"
    public static String speechToTextSecretName = "speechtotextkey"; // Your secret key, (can be found when signed in to Azure Portal under Speech service, Keys and Endpoint)

    //Blob storage
    public static String accountSecretName = "accountname";
    public static String accountSecretKey = "accountkey";
    public static String audioSourceContainerName = "wavfiles"; // The blob container name where the wav-files are saved
    public static String textSaveContainerName = "report"; // The blob container name where the text files will be saved to
    public static String powerBiContainerName = "pwrbi"; // The blob container name where the finished JSON saved will be saved
    public static String transcriptionDestinationContainerName = "temp"; // The blob container name where the transcription results will be saved
    public static String sasTokenSecretName = "sastoken"; // Your secret key, (can be generated when signed in to Azure Portal under Storage account, Shared access signature)
    public static String blobStorageEndpoint = "blobstorageendpoint"; // Your blob storage endpoint

    //OpenAI
    public static String openaiSecretName = "openaikey"; // Your secret key, (can be found when signed in to Azure Portal under Azure OpenAi, Keys and Endpoint)
    public static String openaiEndpoint = "openaiendpoint"; // Your endpoint, (can be found when signed in to Azure Portal under Azure OpenAi, Keys and Endpoint)
    public static String openaiModel = "testGpt4"; // Your AI model, (can be found and set up when signed in to Azure Portal under Azure OpenAi, Model deployments)

    //Utils
    public static String pathToTemp = "src/main/temp/";
    public static String jsonSaveDirectory = "src/main/temp/";
    public static String transcribedTextSaveDirectory = "src/main/temp/";
    public static String sasTokenTranscriptionBlobSecretName = "sasTokenTranscriptionBlob";

    public static String transcriptionApiUrl = "https://swedencentral.api.cognitive.microsoft.com/speechtotext/v3.1/transcriptions/";
}
