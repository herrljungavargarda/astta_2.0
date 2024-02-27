package se.herrljunga.astta.utils;

import java.util.List;

public class Config {
    //Speech To Text
    public static String speechToTextRegion = "swedencentral";
    public static List<String> supportedLanguages = List.of("sv-SE", "en-US", "de-DE", "fr-FR"); // Languages the AI supports (first is base language)

    //Blob storage
    public static String audioSourceContainerName = "wavfiles";
    public static String textSaveContainerName = "textfiles";
    public static String powerBiContainerName = "pwrbi";

    //Utils
    public static String pathToTemp = "src/main/temp/";
    public static String jsonSaveDirectory = "src/main/temp/";
    public static String transcribedTextSaveDirectory = "src/main/temp/";
}
