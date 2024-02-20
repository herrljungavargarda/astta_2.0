package se.herrljunga.astta;

import com.microsoft.cognitiveservices.speech.AutoDetectSourceLanguageConfig;
import se.herrljunga.astta.analyze.OpenAIAnalyzer;
import se.herrljunga.astta.filehandler.StorageHandler;
import se.herrljunga.astta.filehandler.BlobStorageHandler;
import se.herrljunga.astta.speechtotext.SpeechToText;
import se.herrljunga.astta.speechtotext.SpeechToTextImpl;

import java.nio.file.Files;
import java.nio.file.Paths;
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

        try {
            // AI analyzer of text:

            //List<String> paths = testBlobStorage.fetchFile();
            //List<String[]> result = new ArrayList<>();
            String filePath = "src/main/resources/exampleText.txt";


            String result = Files.readAllLines(Paths.get(filePath))
                    .stream().collect(Collectors.joining(System.lineSeparator()));

            OpenAIAnalyzer analyzer = new OpenAIAnalyzer(Config.openAiKey, Config.openAiEndpoint, "test");
            analyzer.analyze(result, "sv-SE");
            // Remove sensitive data:

            //Analyze analyze = new AnalyzeImpl(Config.languageKey, Config.languageEndpoint);
            //analyze.removeSensitiveInformation("");


            // Transcribe:

            //List<String> paths = testBlobStorage.fetchFile();
            //List<String[]> result = new ArrayList<>();
            //for (var path : paths) {
            //    System.out.println("path " + path);
            //    String[] transcribedCall = speechToText.speechToText(path);
            //    result.add(transcribedCall);
            //}
            //speechToText.close();
//
            //for (var call : result) {
            //    System.out.println(call);
            //}
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        } finally {
            System.exit(0);
        }

        //stt.speechToText(result.get(0));

    }
}