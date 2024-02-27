package se.herrljunga.astta.analyze;

import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.TextAnalyticsClientBuilder;
import com.azure.ai.textanalytics.models.PiiEntityCollection;
import com.azure.core.credential.AzureKeyCredential;

public class AnalyzeImpl implements Analyze {
    TextAnalyticsClient client;

    public AnalyzeImpl(String key, String endpoint) {
        client = new TextAnalyticsClientBuilder()
                .credential(new AzureKeyCredential(key))
                .endpoint(endpoint)
                .buildClient();
    }

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
