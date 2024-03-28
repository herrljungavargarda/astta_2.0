package se.herrljunga.astta.speechtotext;

import com.azure.storage.common.StorageSharedKeyCredential;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import se.herrljunga.astta.filehandler.BlobStorageHandler;
import se.herrljunga.astta.filehandler.StorageHandler;
import se.herrljunga.astta.keyvault.KeyVault;
import se.herrljunga.astta.utils.Config;
import se.herrljunga.astta.utils.GenerateSasToken;
import se.herrljunga.astta.utils.Utils;

import java.io.IOException;

public class BatchTranscriber {
    private String speechToTextKey;
    private String audioSourceContainerUrl;
    private String destinationContainerUrl;

    public BatchTranscriber(){
        this.speechToTextKey = KeyVault.getSecret(Config.speechToTextSecretName);
        this.audioSourceContainerUrl = KeyVault.getSecret(Config.blobStorageEndpoint) + "/" + Config.audioSourceContainerName + "?" + KeyVault.getSecret(Config.sasTokenSecretName);


        StorageHandler tempContainer = new BlobStorageHandler(KeyVault.getSecret(Config.blobStorageEndpoint),
                KeyVault.getSecret(Config.sasTokenSecretName),
                new StorageSharedKeyCredential(KeyVault.getSecret(Config.accountSecretName), KeyVault.getSecret(Config.accountSecretKey)));
        var containerClient = tempContainer.createTempContainer(Config.tempContainerName);
        String sasToken = GenerateSasToken.generateSasToken(containerClient);
        destinationContainerUrl = KeyVault.getSecret(Config.blobStorageEndpoint) + "/" + Config.tempContainerName + "?" + sasToken;
    }

    // TODO ???? Convert to async ?????
    public void startTranscription() throws IOException, InterruptedException {
        String response = batchTranscribe();
        String transcriptionUrl = Utils.getElementFromJson(response, "self");
        getTranscriptionStatus(transcriptionUrl);
        System.out.print("Transcribing ");
        while (!getTranscriptionStatus(transcriptionUrl)) {
            System.out.print("֍");
            Thread.sleep(5000);
        }
        System.out.println("\n");
    }

    private String batchTranscribe() throws IOException {
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
        }

    }

    @NotNull
    private JsonObject createRequestBody() {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("contentContainerUrl", audioSourceContainerUrl);
        jsonBody.addProperty("locale", "sv-SE");
        jsonBody.addProperty("displayName", "My Transcription");

        JsonObject properties = new JsonObject();
        //properties.addProperty("wordLevelTimestampsEnabled", true);
        properties.addProperty("destinationContainerUrl", destinationContainerUrl);
        properties.addProperty("diarizationEnabled", true);
        properties.addProperty("timeToLive", "PT12H");

        JsonObject languageIdentification = new JsonObject();
        languageIdentification.add("candidateLocales", new Gson().toJsonTree(new String[]{"sv-SE", "en-US"}));
        properties.add("languageIdentification", languageIdentification);

        jsonBody.add("properties", properties);
        return jsonBody;
    }

    private String getTranscriptionStatusResponse(String transcriptionUrl) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(transcriptionUrl)
                .method("GET", null)
                .addHeader("Ocp-Apim-Subscription-Key", speechToTextKey)
                .build();
        try (Response response = client.newCall(request).execute();) {
            assert response.body() != null;
            return response.body().string();
        }
    }

    // Returns true if transcription is done
    private boolean getTranscriptionStatus(String transcriptionUrl) throws IOException {
        String statusResponse = getTranscriptionStatusResponse(transcriptionUrl);
        String status = Utils.getElementFromJson(statusResponse, "status");
        return switch (status) {
            case "Running" -> false;
            default -> true;
        };
    }
}
