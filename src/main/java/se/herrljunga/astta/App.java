package se.herrljunga.astta;

import com.microsoft.cognitiveservices.speech.AutoDetectSourceLanguageConfig;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.herrljunga.astta.analyze.AnalyzeResult;
import se.herrljunga.astta.analyze.OpenAIAnalyzer;
import se.herrljunga.astta.filehandler.BlobStorageHandler;
import se.herrljunga.astta.filehandler.StorageHandler;
import se.herrljunga.astta.keyvault.KeyVault;
import se.herrljunga.astta.speechtotext.SpeechToText;
import se.herrljunga.astta.speechtotext.SpeechToTextImpl;
import se.herrljunga.astta.utils.AnalyzedCall;
import se.herrljunga.astta.utils.Config;
import se.herrljunga.astta.utils.TranscribedTextAndLanguage;
import se.herrljunga.astta.utils.Utils;

import java.util.List;


public class App {
    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger(App.class);
        logger.debug("Starting logger");

        // Convert audio to text using Azure Speech-to-Text service
        SpeechToText speechToText = new SpeechToTextImpl(
                KeyVault.getSecret(Config.speechToTextSecretName), // Azure Speech service key
                Config.speechToTextRegion, // Azure Speech service region
                Config.supportedLanguages.get(0), // Base language of the speech
                AutoDetectSourceLanguageConfig.fromLanguages(Config.supportedLanguages) // In case the base language is wrong
        );
        // Fetch audio files from Azure Blob Storage
        StorageHandler audioSourceBlobStorage = new BlobStorageHandler(KeyVault.getSecret(Config.blobStorageEndpoint),
                KeyVault.getSecret(Config.sasTokenSecretName),
                Config.audioSourceContainerName);
        StorageHandler textformatBlobStorage = new BlobStorageHandler(KeyVault.getSecret(Config.blobStorageEndpoint),
                KeyVault.getSecret(Config.sasTokenSecretName),
                Config.textSaveContainerName);
        StorageHandler powerBiBlobStorage = new BlobStorageHandler(KeyVault.getSecret(Config.blobStorageEndpoint),
                KeyVault.getSecret(Config.sasTokenSecretName),
                Config.powerBiContainerName);

        OpenAIAnalyzer analyzer = new OpenAIAnalyzer(KeyVault.getSecret(Config.openaiSecretName), KeyVault.getSecret(Config.openaiEndpoint), Config.openaiModel);
        try {
            // Transcribe:
            System.out.println("Getting audio files from Blob Storage...");
            List<String> paths = audioSourceBlobStorage.fetchFile();
            for (var audioFile : paths) {

                if (audioFile.contains("testwav")) {

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
            }
        } catch (Exception e) {
            e.printStackTrace();

            logger.error("Exception occured: ", e);
        } finally {
            System.exit(0);
        }
    }
}