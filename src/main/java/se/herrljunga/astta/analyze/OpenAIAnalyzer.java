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
    private String tokensUsed;

    /**
     * Gets the tokens used in the last analysis request.
     *
     * @return
     */

    public String getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(String tokensUsed) {
        this.tokensUsed = tokensUsed;
    }

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
     * Analyze the given text using the OpenAI API.
     *
     * @param textToAnalyze The text to be analyzed.
     * @param language      The language of the text.
     * @return The analyzed text.
     */

    public TranscribedTextAndLanguage analyze(TranscribedTextAndLanguage textToAnalyze) {
        List<ChatRequestMessage> chatMessages = new ArrayList<>();
        String filePath = "src/main/resources/prompt.txt";
        try {
            String mainPrompt = Files.readAllLines(Paths.get(filePath)).stream().collect(Collectors.joining(System.lineSeparator()));
            chatMessages.add(new ChatRequestSystemMessage("CLEAR ALL PREVIOUS CHAT HISTORY AND SESSIONS"));
            chatMessages.add(new ChatRequestSystemMessage(mainPrompt));
            chatMessages.add(new ChatRequestSystemMessage("Spoken language: " + textToAnalyze.getLanguage()));
            chatMessages.add(new ChatRequestUserMessage(textToAnalyze.getTranscribedText()));
            ChatCompletions chatCompletions = client.getChatCompletions(deploymentOrModelId, new ChatCompletionsOptions(chatMessages));

            StringBuilder sb = new StringBuilder();
            for (ChatChoice choice : chatCompletions.getChoices()) {
                ChatResponseMessage message = choice.getMessage();
                sb.append(message.getContent()).append("\n");
            }
            CompletionsUsage usage = chatCompletions.getUsage();

            setTokensUsed("Number of prompt token is: " + usage.getPromptTokens() + " number of completion token is: " + usage.getCompletionTokens() + " and number of total tokens in request and response is: " + usage.getTotalTokens());
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
