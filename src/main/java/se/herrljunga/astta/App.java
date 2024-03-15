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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class App {
        // Convert audio to text using Azure Speech-to-Text service
        static SpeechToText speechToText = new SpeechToTextImpl(
                KeyVault.getSecret(Config.speechToTextSecretName), // Azure Speech service key
                Config.speechToTextRegion, // Azure Speech service region
                Config.supportedLanguages.get(0), // Base language of the speech
                AutoDetectSourceLanguageConfig.fromLanguages(Config.supportedLanguages) // In case the base language is wrong
        );
        // Fetch audio files from Azure Blob Storage
        static StorageHandler audioSourceBlobStorage = new BlobStorageHandler(KeyVault.getSecret(Config.blobStorageEndpoint),
                KeyVault.getSecret(Config.sasTokenSecretName),
                Config.audioSourceContainerName);
        static StorageHandler textformatBlobStorage = new BlobStorageHandler(KeyVault.getSecret(Config.blobStorageEndpoint),
                KeyVault.getSecret(Config.sasTokenSecretName),
                Config.textSaveContainerName);
        static StorageHandler powerBiBlobStorage = new BlobStorageHandler(KeyVault.getSecret(Config.blobStorageEndpoint),
                KeyVault.getSecret(Config.sasTokenSecretName),
                Config.powerBiContainerName);

        static OpenAIAnalyzer analyzer = new OpenAIAnalyzer(KeyVault.getSecret(Config.openaiSecretName), KeyVault.getSecret(Config.openaiEndpoint), Config.openaiModel);

    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger(App.class);
        logger.debug("Starting logger");

        List<Thread> threads = new ArrayList<>();



        try {
            // Transcribe:
            List<String> paths = audioSourceBlobStorage.fetchFile();
            List<TranscribedTextAndLanguage> transcribedCalls = new ArrayList<>();
            for (var audioFile : paths){
                System.out.println("Transcribing: " + audioFile + "...");

                TranscribedTextAndLanguage transcribedCall = speechToText.speechToText(audioFile);
                transcribedCalls.add(transcribedCall);

                String transcribedCallSavePath = Config.transcribedTextSaveDirectory +    // src/main/temp
                        Utils.getFileName(audioFile) // Adds the filename of the audiofile (removes path)
                        + ".txt"; // Make it a txt file
                Utils.writeToFile(transcribedCallSavePath, transcribedCall.getTranscribedText());

                textformatBlobStorage.saveToStorage(transcribedCallSavePath);
            }

            for (int i = 0; i < paths.size(); i++) {

                int finalI = i;
                Thread thread = new Thread(() ->
                {
                    try {
                        goooo(paths.get(finalI), transcribedCalls.get(finalI));
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
                thread.start();
                threads.add(thread);
            }

            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // Vänta på att alla trådar ska slutföra

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception occured: ", e);
        } finally {
            System.exit(0);
        }

    }
    public static void goooo(String audioFile, TranscribedTextAndLanguage transcribedCall) throws ExecutionException, InterruptedException {


        AnalyzeResult analyzedCallResult = analyzer.analyze(transcribedCall);

        String analyzedCallJson = Utils.createJson(analyzedCallResult.result(), transcribedCall.getLanguage(), Utils.getAudioDuration(audioFile), analyzedCallResult.tokensUsed());
        String analyzedCallJsonPath = Config.jsonSaveDirectory +    // The json save location folder
                Utils.getFileName(audioFile) // Adds the filename of the audiofile (removes path)
                + ".json"; // Make it a json file
        AnalyzedCall analyzedCall = new AnalyzedCall(analyzedCallJsonPath, analyzedCallJson);
        Utils.writeToFile(analyzedCall);
        //powerBiBlobStorage.saveToStorage(analyzedCallJsonPath); //src/main/temp/file.json

    }
}
