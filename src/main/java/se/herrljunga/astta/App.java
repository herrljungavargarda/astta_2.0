package se.herrljunga.astta;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.google.gson.*;
import com.microsoft.cognitiveservices.speech.AutoDetectSourceLanguageConfig;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.herrljunga.astta.analyze.AnalyzeResult;
import se.herrljunga.astta.analyze.OpenAIAnalyzer;
import se.herrljunga.astta.filehandler.BlobStorageHandler;
import se.herrljunga.astta.filehandler.StorageHandler;
import se.herrljunga.astta.keyvault.KeyVault;
import se.herrljunga.astta.speechtotext.BatchTranscriber;
import se.herrljunga.astta.speechtotext.SpeechToText;
import se.herrljunga.astta.speechtotext.SpeechToTextImpl;
import se.herrljunga.astta.utils.AnalyzedCall;
import se.herrljunga.astta.utils.Config;
import se.herrljunga.astta.utils.TranscribedCallInformation;
import se.herrljunga.astta.utils.Utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class App {
    static StorageHandler textformatBlobStorage = new BlobStorageHandler(KeyVault.getSecret(Config.blobStorageEndpoint),
            KeyVault.getSecret(Config.sasTokenSecretName),
            Config.textSaveContainerName);
    static StorageHandler powerBiBlobStorage = new BlobStorageHandler(KeyVault.getSecret(Config.blobStorageEndpoint),
            KeyVault.getSecret(Config.sasTokenSecretName),
            Config.powerBiContainerName);

    static StorageHandler transcriptionDestinationBlobStorage = new BlobStorageHandler(KeyVault.getSecret(Config.blobStorageEndpoint),
            KeyVault.getSecret(Config.sasTokenSecretName),
            Config.transcriptionDestinationContainerName);

    static StorageHandler createTempContainer = new BlobStorageHandler(KeyVault.getSecret(Config.blobStorageEndpoint),
            KeyVault.getSecret(Config.sasTokenSecretName),
            new StorageSharedKeyCredential(KeyVault.getSecret(Config.accountSecretName), KeyVault.getSecret(Config.accountSecretKey)));

    static OpenAIAnalyzer analyzer = new OpenAIAnalyzer(KeyVault.getSecret(Config.openaiSecretName), KeyVault.getSecret(Config.openaiEndpoint), Config.openaiModel);

    public static void main(String[] args) throws IOException, InterruptedException {


        Logger logger = LoggerFactory.getLogger(App.class);
        logger.debug("Starting logger");


        try {
            createTempContainer.createTempContainer(Config.transcriptionDestinationContainerName);
            BatchTranscriber.startTranscription();
            // Transcribe:

            List<String> paths = transcriptionDestinationBlobStorage.fetchFile();
            List<TranscribedCallInformation> transcribedCalls = new ArrayList<>();
            for (var path : paths) {
                if (path.contains("_report")) {
                    textformatBlobStorage.saveToStorage(path);
                }
                if (!path.contains("_report")) {
                    String content = Files.readString(Paths.get(path));

                    JsonElement jsonElement = JsonParser.parseString(content);
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    JsonArray jsonArray = jsonObject.getAsJsonArray("combinedRecognizedPhrases");

                    String transcription = null;

                    for (JsonElement element : jsonArray) {
                        JsonObject combinedRecognizedPhrase = element.getAsJsonObject();
                        transcription = combinedRecognizedPhrase.get("display").getAsString();
                    }
                    String duration = Utils.getElementFromJson(content, "duration");

                    TranscribedCallInformation transcribedCall = new TranscribedCallInformation(transcription, duration, path);
                    transcribedCalls.add(transcribedCall);

                }
            }
            startMultiThreadedAnalysis(transcribedCalls);
            transcriptionDestinationBlobStorage.deleteContainer();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception occured: ", e);
        } finally {
            System.exit(0);
        }

    }

    /**
     * Starts multi-threaded analysis of transcribed calls.
     * <p>
     * This method uses an ExecutorService to manage threads. Each transcribed call is analyzed in a separate thread.
     * The method waits for all threads to complete before it returns.
     *
     * @param transcribedCalls a list of transcribed calls to be analyzed
     */
    private static void startMultiThreadedAnalysis(List<TranscribedCallInformation> transcribedCalls) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<?>> futures = new ArrayList<>();

        for (var call : transcribedCalls) {
            Future<?> future = executorService.submit(() -> {
                try {
                    AnalyzeResult analyzedCallResult = analyzer.getAnalyzeResult(call);
                    buildJsonFile(analyzedCallResult, call);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        executorService.shutdown(); // Always remember to shutdown the executor service
    }

    public static void buildJsonFile(AnalyzeResult analyzedCallResult, TranscribedCallInformation transcribedCall) throws ExecutionException, InterruptedException {
        String analyzedCallJson = Utils.createJson(analyzedCallResult.result(), transcribedCall.getCallDuration(), analyzedCallResult.tokensUsed());
        String analyzedCallJsonPath = Config.jsonSaveDirectory +    // The json save location folder
                Utils.getFileName(transcribedCall.getPath()) // Adds the filename of the audiofile (removes path)
                + ".json"; // Make it a json file
        AnalyzedCall analyzedCall = new AnalyzedCall(analyzedCallJsonPath, analyzedCallJson);
        Utils.writeToFile(analyzedCall);
        powerBiBlobStorage.saveToStorage(analyzedCallJsonPath); //src/main/temp/file.json
    }
}
