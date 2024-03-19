package se.herrljunga.astta.speechtotext;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import se.herrljunga.astta.keyvault.KeyVault;
import se.herrljunga.astta.utils.Config;
import se.herrljunga.astta.utils.Utils;

import java.io.IOException;

public class BatchTranscriber {
    private static String speechToTextKey = KeyVault.getSecret(Config.speechToTextSecretName);

    private static String audioSourceContainerUrl = KeyVault.getSecret(Config.blobStorageEndpoint) + "/" + Config.audioSourceContainerName + "?" + KeyVault.getSecret(Config.sasTokenSecretName);
    private static String destinationContainerUrl = KeyVault.getSecret(Config.blobStorageEndpoint) + "/" + Config.transcriptionDestinationContainername + "?" + KeyVault.getSecret(Config.sasTokenTranscriptionBlobSecretName);

    public static void startTranscription() throws IOException, InterruptedException {
        String response = batchTranscribe();
        String transcriptionUrl = Utils.getElementFromJson(response, "self");
        getTranscriptionStatus(transcriptionUrl);
        while (!getTranscriptionStatus(transcriptionUrl)) {
            System.out.println(getTranscriptionStatus(transcriptionUrl));
            Thread.sleep(10000);
        }
    }

    private static String batchTranscribe() throws IOException {
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
            return response.body().string();
        }

    }

    @NotNull
    private static JsonObject createRequestBody() {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("contentContainerUrl", audioSourceContainerUrl);
        jsonBody.addProperty("locale", "sv-SE");
        jsonBody.addProperty("displayName", "My Transcription");

        JsonObject properties = new JsonObject();
        properties.addProperty("wordLevelTimestampsEnabled", true);
        properties.addProperty("destinationContainerUrl", destinationContainerUrl);

        JsonObject languageIdentification = new JsonObject();
        languageIdentification.add("candidateLocales", new Gson().toJsonTree(new String[]{"sv-SE", "en-US"}));
        properties.add("languageIdentification", languageIdentification);

        jsonBody.add("properties", properties);
        return jsonBody;
    }

    private static String getTranscriptionStatusResponse(String transcriptionUrl) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(transcriptionUrl)
                .method("GET", null)
                .addHeader("Ocp-Apim-Subscription-Key", speechToTextKey)
                .build();
        try (Response response = client.newCall(request).execute();) {
            return response.body().string();
        }
    }

    // Returns true if transcription is done
    private static boolean getTranscriptionStatus(String transcriptionUrl) throws IOException {
        String statusResponse = getTranscriptionStatusResponse(transcriptionUrl);
        String status = Utils.getElementFromJson(statusResponse, "status");
        switch (status) {
            case "Succeeded":
                return true;
            case "Failed":
                throw new RuntimeException("Failed transcription");
            default:
                return false;
        }
    }
}
