package se.herrljunga.astta.analyze;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.core.http.policy.RetryStrategy;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.herrljunga.astta.utils.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The OpenAIAnalyzer class provides functionality to analyze text using the OpenAI API.
 * It includes methods to analyze transcribed text, extract information from transcribed files,
 * build a JSON file from the result of an analyzed call, and get the analysis result of a transcribed call.
 * <p>
 * The class is initialized with an API key, endpoint, and deployment or model ID for the OpenAI service.
 * It uses a custom HttpClient with a modified response timeout and a RetryPolicy for handling retries.
 */
public class OpenAIAnalyzer {
    private static Config config = ConfigLoader.loadConfig();
    private final OpenAIClient client;
    private final String deploymentOrModelId;
    private final Logger logger = LoggerFactory.getLogger(OpenAIAnalyzer.class);

    /**
     * Constructs a new OpenAIAnalyzer with the specified API key, endpoint, and deployment or model ID.
     * It initializes the OpenAIClient with a custom HttpClient and RetryPolicy.
     * The HttpClient has a modified response timeout of 2 minutes.
     * The RetryPolicy has a delay of 10 seconds and a maximum of 3 retry attempts.
     *
     * @param openAiKey           The API key for accessing the OpenAI service.
     * @param openAiEndpoint      The endpoint URL of the OpenAI service.
     * @param deploymentOrModelId The deployment or model ID to interact with.
     */
    public OpenAIAnalyzer(String openAiKey, String openAiEndpoint, String deploymentOrModelId) {

        RetryPolicy retryPolicy = new RetryPolicy(new RetryStrategy() {
            @Override
            public Duration calculateRetryDelay(int retryAttempts) {
                return Duration.ofSeconds(10); // Set the delay to 120 seconds
            }

            @Override
            public int getMaxRetries() {
                return 3; // Set the maximum number of retry attempts to 3
            }
        });

        // Create a custom HttpClient with a modified timeout
        HttpClient httpClient = new NettyAsyncHttpClientBuilder()
                .responseTimeout(Duration.ofMinutes(2))
                .build();

        this.client = new OpenAIClientBuilder().credential(new AzureKeyCredential(openAiKey)).endpoint(openAiEndpoint).httpClient(httpClient).retryPolicy(retryPolicy).buildClient();
        this.deploymentOrModelId = deploymentOrModelId;
        logger.info("OpenAIAnalyzer initialized with deployment/model ID: {}", deploymentOrModelId);
    }

    /**
     * Analyzes the transcribed text using the OpenAI API.
     * <p>
     * This method reads a prompt from a file, sends a series of chat messages to the OpenAI API, and collects the responses.
     * The chat messages include a system message to clear the cache, a system message containing the main prompt, and a user message containing the transcribed text.
     * The responses from the OpenAI API are concatenated into a single string.
     * The total number of tokens used in the analysis is also recorded.
     *
     * @param transcribedCallInformation The transcribed call information, which includes the transcribed text.
     * @return An AnalyzeResult object containing the analysis result and the number of tokens used.
     * @throws RuntimeException if an IOException occurs when reading the prompt file or if an error occurs during the analysis.
     */
    public AnalyzeResult analyze(TranscribedCallInformation transcribedCallInformation) {
        List<ChatRequestMessage> chatMessages = new ArrayList<>();
        String promptPath = config.openAI.promptPath;
        InputStream in = getClass().getResourceAsStream(promptPath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String mainPrompt = reader.lines().collect(Collectors.joining(System.lineSeparator()));
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

        logger.info("Analysis of {} completed successfully. Total tokens used: {}", Utils.removePathFromFilename(transcribedCallInformation.getPath()), usage.getTotalTokens());
        return new AnalyzeResult(sb.toString(), usage.getTotalTokens());
    }

    /**
     * This method attempts to analyze a transcribed call up to three times.
     * It uses the analyze() method to perform the analysis and checks if the result is a valid JSON.
     * If the result is not a valid JSON, it retries the analysis up to three times.
     * If after three attempts the result is still not a valid JSON, it throws a RuntimeException.
     *
     * @param transcribedCall The transcribed call information, which includes the transcribed text, its duration, and the path to the file.
     * @return An AnalyzeResult object containing the analysis result and the number of tokens used.
     * @throws RuntimeException if after three attempts the result of the analysis is still not a valid JSON.
     */
    @NotNull
    public AnalyzeResult getAnalyzeResult(TranscribedCallInformation transcribedCall) {
        AnalyzeResult analyzedCallResult;
        for (int i = 1; true; i++) {
            logger.info("Analyzing {} attempt: {}", Utils.removePathFromFilename(transcribedCall.getPath()), i);
            analyzedCallResult = analyze(transcribedCall);

            if (Utils.validateJson(analyzedCallResult.result())) {
                break;
            } else if (i == 3) {
                throw new RuntimeException("Couldn't create valid JSON file after 3 tries.");
            }

        }
        return analyzedCallResult;
    }

    /**
     * Build json file out of the analyzed call
     *
     * @param analyzedCallResult the result of the analyzed call
     * @param transcribedCall    the transcribed call
     * @return final result of the analyzed call in .json format
     */
    public AnalyzedCall buildJsonFile(AnalyzeResult analyzedCallResult, TranscribedCallInformation transcribedCall) {
        String analyzedCallJson = Utils.createJson(analyzedCallResult.result(), transcribedCall.getCallDuration(), analyzedCallResult.tokensUsed(), transcribedCall.getPath());
        String analyzedCallJsonPath = config.utils.analyzedJsonSaveDirectory +    // The json save location folder
                Utils.getFileName(transcribedCall.getPath()) // Adds the filename of the audiofile (removes path)
                + ".json"; // Make it a json file
        AnalyzedCall analyzedCall = new AnalyzedCall(analyzedCallJsonPath, analyzedCallJson);
        Utils.writeToFile(analyzedCall);
        return analyzedCall;
    }

    /**
     * Extracting the information needed from the transcribed file (json start at 10k lines)
     *
     * @param paths A list of file paths to the transcribed files.
     * @return A list of TranscribedCallInformation objects, each containing the transcription, duration, and path of a transcribed call.
     * @throws RuntimeException if an IOException occurs when reading the file or parsing the JSON content.
     */
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
        } catch (IOException e) {
            LoggerFactory.getLogger(OpenAIAnalyzer.class).error("An error occurred when extracting information from {}, {}", paths, e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        return transcribedCalls;
    }
}