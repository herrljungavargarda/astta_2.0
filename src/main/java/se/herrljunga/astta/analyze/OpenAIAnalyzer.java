package se.herrljunga.astta.analyze;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import se.herrljunga.astta.utils.TranscribedTextAndLanguage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The OpenAIAnalyzer class provides functionality to analyze text using the OpenAI API.
 */

public class OpenAIAnalyzer {
    private OpenAIClient client;
    private String deploymentOrModelId;

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

    }

    /**
     * Analyzes transcribed text and language to generate chat completions and usage statistics.
     *
     * @param transcribedTextAndLanguage the transcribed text along with its language
     * @return an AnalyzeResult object containing chat completions and usage statistics
     * The main prompt can be found under src/main/resources/prompt.txt
     * If you wish to change the output format or add/remove anything you can edit that file.
     * We would recommend to follow the prompt style used.
     */

    public AnalyzeResult analyze(TranscribedTextAndLanguage transcribedTextAndLanguage) {
        List<ChatRequestMessage> chatMessages = new ArrayList<>();
        String filePath = "src/main/resources/prompt.txt";
        try {
            String mainPrompt = Files.readAllLines(Paths.get(filePath)).stream().collect(Collectors.joining(System.lineSeparator()));
            chatMessages.add(new ChatRequestSystemMessage("Before continuing, REMOVE OLD CACHE."));
            chatMessages.add(new ChatRequestSystemMessage(mainPrompt));
            chatMessages.add(new ChatRequestSystemMessage("Spoken language: " + transcribedTextAndLanguage.getLanguage()));
            chatMessages.add(new ChatRequestUserMessage(transcribedTextAndLanguage.getTranscribedText()));
            ChatCompletions chatCompletions = client.getChatCompletions(deploymentOrModelId, new ChatCompletionsOptions(chatMessages));

            StringBuilder sb = new StringBuilder();
            for (ChatChoice choice : chatCompletions.getChoices()) {
                ChatResponseMessage message = choice.getMessage();
                sb.append(message.getContent()).append("\n");
            }
            CompletionsUsage usage = chatCompletions.getUsage();

            return new AnalyzeResult(sb.toString(), usage.getTotalTokens());
        } catch (IOException e) {
            System.err.println("An error reading prompt.txt: " + e.getMessage());
            throw new RuntimeException("Exception thrown in OpenAiAnalyzer, analyze " + e.getMessage());
        }
    }
}
