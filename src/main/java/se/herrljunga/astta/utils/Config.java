package se.herrljunga.astta.utils;

import java.util.List;

public class Config {
    //Speech To Text
    public static String speechToTextKey = "81f88cc4da4e42588e8aeb405a291572";
    public static String speechToTextRegion = "swedencentral";
    public static List<String> supportedLanguages = List.of("sv-SE", "en-US", "de-DE", "fr-FR"); // Languages the AI supports (first is base language)

    //Blob storage
    public static String blobSasToken = "?sv=2022-11-02&ss=bfqt&srt=sco&sp=rwdlacupiytfx&se=2024-02-23T18:23:05Z&st=2024-02-19T08:23:05Z&spr=https&sig=8v1wtSuKBSNo2%2BklXTSXoBxWjZveTTMI1VV9YeLPvuk%3D ";
    public static String blobStorageEndpoint = "https://hvprdrg20audio.blob.core.windows.net";
    public static String audioSourceContainerName = "test";
    public static String textSaveContainerName = "textformat";

    //Language analyze
    public static String languageEndpoint = "https://hv-prd-rg20-language.cognitiveservices.azure.com/";
    public static String languageKey = "9324d033fc4644f488c547ff4c1c39cd";

    //Utils
    public static String pathToTemp = "src/main/temp/";

    //OpenAI
    public static String openAiKey = "e3d53c5b1ff64141b33901231d2b9256";
    public static String openAiEndpoint = "https://hv-prd-rg20-azureopenai.openai.azure.com/";
}

