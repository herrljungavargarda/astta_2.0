package se.herrljunga.astta.utils;

import java.util.List;

/**
 * The Config class.
 *
 * This class provides configuration settings for the application.
 * It includes settings for Azure Speech to Text service, Azure Blob Storage, Azure OpenAI, and other utilities.
 * The settings are stored as public static fields, so they can be accessed directly from other classes.
 */
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
    public static String tempContainerName = "temp"; // The blob container name where the transcription results will be saved
    public static String sasTokenSecretName = "sastoken"; // Your secret key, (can be generated when signed in to Azure Portal under Storage account, Shared access signature)
    public static String blobStorageEndpoint = "blobstorageendpoint"; // Your blob storage endpoint

    //OpenAI
    public static String openaiSecretName = "openaikey"; // Your secret key, (can be found when signed in to Azure Portal under Azure OpenAi, Keys and Endpoint)
    public static String openaiEndpoint = "openaiendpoint"; // Your endpoint, (can be found when signed in to Azure Portal under Azure OpenAi, Keys and Endpoint)
    public static String openaiModel = "gpt-4"; // Your AI model, (can be found and set up when signed in to Azure Portal under Azure OpenAi, Model deployments)

    //Utils
    public static String pathToTemp = "src/main/temp/";
    public static String analyzedJsonSaveDirectory = "src/main/temp/analyzed/";
    public static String transcribedTextSaveDirectory = "src/main/temp/";
    public static String sasTokenTranscriptionBlobSecretName = "sasTokenTranscriptionBlob";

    public static String transcriptionApiUrl = "https://swedencentral.api.cognitive.microsoft.com/speechtotext/v3.1/transcriptions/";
    public static String transcriptionsApiUrlPreview = "https://swedencentral.api.cognitive.microsoft.com/speechtotext/v3.2-preview.2/transcriptions/";

    public static int maxThreadsForAnalysis = 20; //Stress testes (100 files avg length 7min): 60 threads with gpt-35-turbo with 50k TPM limit. 20 threads with gpt-4 with 10k TPM limit. (Run your own tests to see where it's stable)
}
