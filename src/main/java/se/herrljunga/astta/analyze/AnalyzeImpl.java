package se.herrljunga.astta.analyze;

import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.TextAnalyticsClientBuilder;
import com.azure.ai.textanalytics.models.PiiEntityCollection;
import com.azure.core.credential.AzureKeyCredential;

/**
 * Implementation class for erforming text analysis operations.
 */

@Deprecated
public class AnalyzeImpl implements Analyze {
    TextAnalyticsClient client;

    /**
     * Constructs a new AnalyzeImpl instance with the specified Azure Text Analytics key and endpoint.
     *
     * @param key      The Aure Text Analytics API key.
     * @param endpoint The endpoint for the Azure Text Analytics service.
     */
    public AnalyzeImpl(String key, String endpoint) {
        client = new TextAnalyticsClientBuilder()
                .credential(new AzureKeyCredential(key))
                .endpoint(endpoint)
                .buildClient();
    }

    /**
     * Remove sensitive information from the provided text
     *
     * @param text The text which can contain sensitive information.
     * @return The Text with sensitive information removed.
     */

    @Override
    public String removeSensitiveInformation(String text) { // Personnummer i format ex"19450522-1822"
        String document = "Mitt personnummer är 19450522-1822";
        System.out.println(document);
        PiiEntityCollection piiEntityCollection = client.recognizePiiEntities(document, "sv");
        System.out.printf("Redacted Text: %s%n", piiEntityCollection.getRedactedText());
        piiEntityCollection.forEach(entity -> System.out.printf(
                "Recognized Personally Identifiable Information entity: %s, entity category: %s, entity subcategory: %s,"
                        + " confidence score: %f.%n",
                entity.getText(), entity.getCategory(), entity.getSubcategory(), entity.getConfidenceScore()));
        return "";
    }

    @Override
    public String getSummarize(String text) {
        return null;
    }

    @Override
    public String getContext(String text) {
        return null;
    }

    @Override
    public String getScore(String text) {
        return null;
    }
}
