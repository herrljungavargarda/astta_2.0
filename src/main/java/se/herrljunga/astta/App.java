package se.herrljunga.astta;

import com.microsoft.cognitiveservices.speech.AutoDetectSourceLanguageConfig;
import se.herrljunga.astta.analyze.AnalyzeResult;
import se.herrljunga.astta.analyze.OpenAIAnalyzer;
import se.herrljunga.astta.filehandler.StorageHandler;
import se.herrljunga.astta.filehandler.BlobStorageHandler;
import se.herrljunga.astta.speechtotext.SpeechToText;
import se.herrljunga.astta.speechtotext.SpeechToTextImpl;
import se.herrljunga.astta.utils.AnalyzedCall;
import se.herrljunga.astta.utils.Config;
import se.herrljunga.astta.utils.Utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class App {
    public static void main(String[] args) {

        // Convert audio to text using Azure Speech-to-Text service
        SpeechToText speechToText = new SpeechToTextImpl(
                Config.speechToTextKey, // Azure Speech service key
                Config.speechToTextRegion, // Azure Speech service region
                Config.supportedLanguages.get(0), // Base language of the speech
                AutoDetectSourceLanguageConfig.fromLanguages(Config.supportedLanguages) // In case the base language is wrong
        );
        // Fetch audio files from Azure Blob Storage
        StorageHandler testBlobStorage = new BlobStorageHandler(Config.blobStorageEndpoint,
                Config.blobSasToken,
                Config.audioSourceContainerName);
        StorageHandler textformatBlobStorage = new BlobStorageHandler(Config.blobStorageEndpoint,
                Config.blobSasToken,
                Config.textSaveContainerName);
        StorageHandler powerBiBlobStorage = new BlobStorageHandler(Config.blobStorageEndpoint,
                Config.blobSasToken,
                Config.powerBiContainerName);

        try {
            // AI analyzer of text:
//            List<String[]> results = new ArrayList<>();
            //String filePath = "src/main/resources/exampleText.txt";
            //String result = Files.readAllLines(Paths.get(filePath))
            //        .stream().collect(Collectors.joining(System.lineSeparator()));


            // Remove sensitive data:
            //String jsonFilePath = "src/main/resources/call3.json";
            //String s = Utils.createJson(analyzedText, "sv-SE", 123.234, 123);
            //Utils.writeToFile(jsonFilePath, s);
            //powerBiBlobStorage.saveToStorage(jsonFilePath);

            //Analyze analyze = new AnalyzeImpl(Config.languageKey, Config.languageEndpoint);
            //analyze.removeSensitiveInformation("");


            // Transcribe:

            OpenAIAnalyzer analyzer = new OpenAIAnalyzer(Config.openAiKey, Config.openAiEndpoint, "testGpt4");
            List<String> paths = testBlobStorage.fetchFile();
            for (var audioFile : paths) {
                String[] transcribedCall = speechToText.speechToText(audioFile);
                AnalyzeResult analyzedCallResult = analyzer.analyze(transcribedCall[0], transcribedCall[1]);
                String analyzedCallJson = Utils.createJson(analyzedCallResult.result(), transcribedCall[1], Utils.getAudioDuration(audioFile), analyzedCallResult.tokensUsed());

                String analyzedCallJsonPath = Config.jsonSaveDirectory +    // The json save location folder
                        Utils.getFileName(audioFile) // Adds the filename of the audiofile (removes path)
                + ".json"; // Make it a json file

                AnalyzedCall analyzedCall = new AnalyzedCall(analyzedCallJsonPath, analyzedCallJson);
                Utils.writeToFile(analyzedCall);

                //powerBiBlobStorage.saveToStorage(analyzedCallJsonPath); //src/main/temp/file.json
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        } finally {
            System.exit(0);
        }
    }
}