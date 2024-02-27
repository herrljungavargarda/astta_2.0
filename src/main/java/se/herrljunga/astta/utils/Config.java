package se.herrljunga.astta.utils;

import java.util.List;

public class Config {
    //Speech To Text
    public static String speechToTextRegion = "swedencentral";
    public static List<String> supportedLanguages = List.of("sv-SE", "en-US", "de-DE", "fr-FR"); // Languages the AI supports (first is base language)
    public static String speechToTextSecretName = "speechtotextkey";

    //Blob storage
    public static String audioSourceContainerName = "wavfiles";
    public static String textSaveContainerName = "textfiles";
    public static String powerBiContainerName = "pwrbi";
    public static String sasTokenSecretName = "sastoken";
    public static String blobStorageEndpoint = "blobstorageendpoint";

    //OpenAI
    public static String openaiSecretName = "openaikey";
    public static String openaiEndpoint = "openaiendpoint";
    public static String openaiModel = "testGpt4";

    //Utils
    public static String pathToTemp = "src/main/temp/";
    public static String jsonSaveDirectory = "src/main/temp/";
    public static String transcribedTextSaveDirectory = "src/main/temp/";
}
