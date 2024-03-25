package se.herrljunga.astta.analyze;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.herrljunga.astta.App;
import se.herrljunga.astta.utils.AnalyzedCall;
import se.herrljunga.astta.utils.Config;
import se.herrljunga.astta.utils.TranscribedCallInformation;
import se.herrljunga.astta.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * The OpenAIAnalyzer class provides functionality to analyze text using the OpenAI API.
 */
public class OpenAIAnalyzer {
    private final OpenAIClient client;
    private final String deploymentOrModelId;
    private final Logger logger = LoggerFactory.getLogger(OpenAIAnalyzer.class);

    /**
     * Constructs a new OpenAIAnalyzer with the specified API key, endpoint, and deployment or model ID.
     *
     * @param openAiKey           The API key for accessing the OpenAI service.
     * @param openAiEndpoint      The endpoint URL of the OpenAI service.
     * @param deploymentOrModelId The deployment or model ID to interact with.
     */
    public OpenAIAnalyzer(String openAiKey, String openAiEndpoint, String deploymentOrModelId) {
        this.client = new OpenAIClientBuilder().credential(new AzureKeyCredential(openAiKey)).endpoint(openAiEndpoint).buildClient();
        this.deploymentOrModelId = deploymentOrModelId;
        logger.info("OpenAIAnalyzer initialized with deployment/model ID: " + deploymentOrModelId);
    }

    /**
     * Analyzes transcribed text and language to generate chat completions and usage statistics.
     *
     * @param transcribedCallInformation the transcribed text along with its language
     * @return an AnalyzeResult object containing chat completions and usage statistics
     * The main prompt can be found under src/main/resources/prompt.txt
     * If you wish to change the output format or add/remove anything you can edit that file.
     * We would recommend to follow the prompt style used.
     */
    public AnalyzeResult analyze(TranscribedCallInformation transcribedCallInformation) {
        List<ChatRequestMessage> chatMessages = new ArrayList<>();
        String filePath = "src/main/resources/prompt.txt";
        try {
            String mainPrompt = Files.readAllLines(Paths.get(filePath)).stream().collect(Collectors.joining(System.lineSeparator()));
            chatMessages.add(new ChatRequestSystemMessage("Before continuing, REMOVE OLD CACHE."));
            chatMessages.add(new ChatRequestSystemMessage(mainPrompt));
            chatMessages.add(new ChatRequestUserMessage(transcribedCallInformation.getTranscribedText()));
            ChatCompletions chatCompletions = client.getChatCompletions(deploymentOrModelId, new ChatCompletionsOptions(chatMessages));

            StringBuilder sb = new StringBuilder();
            for (ChatChoice choice : chatCompletions.getChoices()) {
                ChatResponseMessage message = choice.getMessage();
                sb.append(message.getContent()).append("\n");
            }
            CompletionsUsage usage = chatCompletions.getUsage();

            logger.info("Analysis of " + Utils.removePathFromFilename(transcribedCallInformation.getPath()) + " completed successfully. Total tokens used: " + usage.getTotalTokens());
            return new AnalyzeResult(sb.toString(), usage.getTotalTokens());
        } catch (IOException e) {
            logger.error("An error occurred when reading prompt.txt: " + e.getMessage());
            throw new RuntimeException("Exception thrown in OpenAiAnalyzer, analyze " + e.getMessage());
        }
    }

    @NotNull
    public AnalyzeResult getAnalyzeResult(TranscribedCallInformation transcribedCall) {
        AnalyzeResult analyzedCallResult;
        for (int i = 1; true; i++) {
            LoggerFactory.getLogger(App.class).info("Analyzing " + Utils.removePathFromFilename(transcribedCall.getPath()) + " attempt: " + i);
            analyzedCallResult = analyze(transcribedCall);

            if (Utils.validateJson(analyzedCallResult.result())) {
                break;
            } else if (i == 3) {
                throw new RuntimeException("Couldn't create valid JSON file");
            }

        }
        return analyzedCallResult;
    }

    public AnalyzedCall buildJsonFile(AnalyzeResult analyzedCallResult, TranscribedCallInformation transcribedCall) throws ExecutionException, InterruptedException {
        String analyzedCallJson = Utils.createJson(analyzedCallResult.result(), transcribedCall.getCallDuration(), analyzedCallResult.tokensUsed());
        String analyzedCallJsonPath = Config.jsonSaveDirectory +    // The json save location folder
                Utils.getFileName(transcribedCall.getPath()) // Adds the filename of the audiofile (removes path)
                + ".json"; // Make it a json file
        return new AnalyzedCall(analyzedCallJsonPath, analyzedCallJson);
    }

    public static List<TranscribedCallInformation> extractInformationFromTranscribedFiles(List<String> paths) {
        List<TranscribedCallInformation> transcribedCalls = new ArrayList<>();
        try {
            for (var path : paths) {
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
        } catch (IOException e) { //TODO: fixa
            throw new RuntimeException(e.getMessage());
        }
        return transcribedCalls;
    }
}