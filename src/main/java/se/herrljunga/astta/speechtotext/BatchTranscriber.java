package se.herrljunga.astta.speechtotext;

import com.azure.storage.common.StorageSharedKeyCredential;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.herrljunga.astta.App;
import se.herrljunga.astta.filehandler.BlobStorageHandler;
import se.herrljunga.astta.filehandler.StorageHandler;
import se.herrljunga.astta.keyvault.KeyVault;
import se.herrljunga.astta.utils.Config;
import se.herrljunga.astta.utils.GenerateSasToken;
import se.herrljunga.astta.utils.Utils;

import java.io.IOException;

/**
 * The BatchTranscriber class.
 *
 * This class provides methods to transcribe audio files in batch using Azure Speech to Text service.
 * It uses the Azure Storage SDK for Java to interact with Azure Blob Storage.
 * The class is initialized with the keys and URLs of the source and destination containers in Azure Blob Storage, which are retrieved from Azure Key Vault.
 * It provides a method to start the transcription process, which includes sending a transcription request to Azure Speech to Text service and checking the status of the transcription.
 */
public class BatchTranscriber {
    private String speechToTextKey;
    private String audioSourceContainerUrl;
    private String destinationContainerUrl;
    Logger logger = LoggerFactory.getLogger(App.class);

    /**
     * Constructs a new BatchTranscriber instance.
     *
     * This constructor initializes the keys and URLs of the source and destination containers in Azure Blob Storage, which are retrieved from Azure Key Vault.
     * It also creates a temporary container in Azure Blob Storage for storing the transcription results.
     */
    public BatchTranscriber() {
        this.speechToTextKey = KeyVault.getSecret(Config.speechToTextSecretName);
        this.audioSourceContainerUrl = KeyVault.getSecret(Config.blobStorageEndpoint) + "/" + Config.audioSourceContainerName + "?" + KeyVault.getSecret(Config.sasTokenSecretName);

        StorageHandler tempContainer = new BlobStorageHandler(KeyVault.getSecret(Config.blobStorageEndpoint),
                KeyVault.getSecret(Config.sasTokenSecretName),
                new StorageSharedKeyCredential(KeyVault.getSecret(Config.accountSecretName), KeyVault.getSecret(Config.accountSecretKey)));
        var containerClient = tempContainer.createTempContainer(Config.tempContainerName);
        String sasToken = GenerateSasToken.generateSasToken(containerClient);
        destinationContainerUrl = KeyVault.getSecret(Config.blobStorageEndpoint) + "/" + Config.tempContainerName + "?" + sasToken;
    }

    /**
     * Starts the transcription process.
     *
     * This method sends a transcription request to Azure Speech to Text service and checks the status of the transcription.
     * It waits for the transcription to complete before it returns.
     */
    public void startTranscription() {
        try {
            String response = batchTranscribe();
            String transcriptionUrl = Utils.getElementFromJson(response, "self");
            getTranscriptionStatus(transcriptionUrl);
            System.out.print("Transcribing ");
            while (!getTranscriptionStatus(transcriptionUrl)) {
                System.out.print("֍");
                Thread.sleep(5000);
            }
            System.out.println("\n");
        } catch (InterruptedException e) {
            logger.error("Error while transcribing files{}", e.getMessage());
            throw new RuntimeException("Exception thrown in BatchTranscriber, startTranscription() " + e.getMessage());
        }
    }

    /**
     * Sends a transcription request to Azure Speech to Text service.
     *
     * This method creates a JSON request body and sends a POST request to Azure Speech to Text service.
     * It returns the response from the service as a string.
     *
     * @return The response from Azure Speech to Text service as a string.
     */
    private String batchTranscribe() {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");

        JsonObject jsonBody = createRequestBody();

        RequestBody body = RequestBody.create(mediaType, jsonBody.toString());
        Request request = new Request.Builder()
                .url(Config.transcriptionApiUrl)
                .method("POST", body)
                .addHeader("Ocp-Apim-Subscription-Key", speechToTextKey)
                .addHeader("Content-Type", "application/json")
                .build();
        try (Response response = client.newCall(request).execute();) {
            assert response.body() != null;
            return response.body().string();
        } catch (IOException e) {
            logger.error("An error occurred when transcribing: {}", e.getMessage());
            throw new RuntimeException("Exception thrown in BatchTranscriber, batchTranscribe " + e.getMessage());
        }
    }

    /**
     * Creates a JSON request body for the transcription request.
     *
     * This method creates a JSON object with the necessary properties for the transcription request.
     * It returns the JSON object.
     *
     * @return The JSON request body as a JsonObject.
     */
    @NotNull
    private JsonObject createRequestBody() {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("contentContainerUrl", audioSourceContainerUrl);
        jsonBody.addProperty("locale", "sv-SE");
        jsonBody.addProperty("displayName", "My Transcription");

        JsonObject properties = new JsonObject();
        properties.addProperty("wordLevelTimestampsEnabled", false);
        properties.addProperty("destinationContainerUrl", destinationContainerUrl);
        properties.addProperty("diarizationEnabled", false);
        properties.addProperty("timeToLive", "PT12H");

        JsonObject languageIdentification = new JsonObject();
        languageIdentification.add("candidateLocales", new Gson().toJsonTree(new String[]{"sv-SE", "en-US"}));
        properties.add("languageIdentification", languageIdentification);

        jsonBody.add("properties", properties);
        return jsonBody;
    }

    /**
     * Gets the status of the transcription.
     *
     * This method sends a GET request to Azure Speech to Text service to get the status of the transcription.
     * It returns true if the transcription is done, and false otherwise.
     *
     * @param transcriptionUrl The URL of the transcription.
     * @return True if the transcription is done, and false otherwise.
     */
    private boolean getTranscriptionStatus(String transcriptionUrl) {
        String statusResponse = getTranscriptionStatusResponse(transcriptionUrl);
        String status = Utils.getElementFromJson(statusResponse, "status");
        return switch (status) {
            case "Running" -> false;
            default -> true;
        };
    }

    /**
     * Sends a GET request to Azure Speech to Text service to get the status of the transcription.
     *
     * This method sends a GET request to Azure Speech to Text service and returns the response as a string.
     *
     * @param transcriptionUrl The URL of the transcription.
     * @return The response from Azure Speech to Text service as a string.
     */
    private String getTranscriptionStatusResponse(String transcriptionUrl) {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(transcriptionUrl)
                .method("GET", null)
                .addHeader("Ocp-Apim-Subscription-Key", speechToTextKey)
                .build();
        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            return response.body().string();
        } catch (IOException e) {
            logger.error("An error occurred when getting transcription status: {}", e.getMessage());
            throw new RuntimeException("Exception thrown in BatchTranscriber, getTranscriptionStatusResponse " + e.getMessage());
        }
    }
}