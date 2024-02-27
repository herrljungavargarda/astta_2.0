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
import se.herrljunga.astta.utils.TranscribedTextAndLanguage;
import se.herrljunga.astta.utils.Utils;

import java.util.List;


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
            System.out.println("Getting audio files from Blob Storage...");
            List<String> paths = testBlobStorage.fetchFile();
            for (var audioFile : paths) {

                System.out.println("Transcribing: " + audioFile + "...");
                TranscribedTextAndLanguage transcribedCall = speechToText.speechToText(audioFile);


                String transcribedCallSavePath = Config.transcribedTextSaveDirectory +    // src/main/temp
                        Utils.getFileName(audioFile) // Adds the filename of the audiofile (removes path)
                        + ".txt"; // Make it a txt file
                Utils.writeToFile(transcribedCallSavePath, transcribedCall.getTranscribedText());

                System.out.println("Saving transcription to blob...");
                textformatBlobStorage.saveToStorage(transcribedCallSavePath);


                System.out.println("Analyzing: " + audioFile + "...");
                AnalyzeResult analyzedCallResult = analyzer.analyze(transcribedCall);

                System.out.println("Creating json file");
                String analyzedCallJson = Utils.createJson(analyzedCallResult.result(), transcribedCall.getLanguage(), Utils.getAudioDuration(audioFile), analyzedCallResult.tokensUsed());
                String analyzedCallJsonPath = Config.jsonSaveDirectory +    // The json save location folder
                        Utils.getFileName(audioFile) // Adds the filename of the audiofile (removes path)
                        + ".json"; // Make it a json file
                AnalyzedCall analyzedCall = new AnalyzedCall(analyzedCallJsonPath, analyzedCallJson);

                System.out.println("Saving " + analyzedCallJsonPath + " to blob storage");
                Utils.writeToFile(analyzedCall);

                //powerBiBlobStorage.saveToStorage(analyzedCallJsonPath); //src/main/temp/file.json
                System.out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        } finally {
            System.exit(0);
        }
    }
}