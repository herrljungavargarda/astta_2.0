package se.herrljunga.astta.analyze;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.herrljunga.astta.utils.AnalyzedCall;
import se.herrljunga.astta.utils.TranscribedCallInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MultiThreadAnalyzer {
    private final OpenAIAnalyzer analyzer;
    private final Logger logger = LoggerFactory.getLogger(OpenAIAnalyzer.class);

    public MultiThreadAnalyzer(OpenAIAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Starts multi-threaded analysis of transcribed calls.
     * <p>
     * This method uses an ExecutorService to manage threads. Each transcribed call is analyzed in a separate thread.
     * The method waits for all threads to complete before it returns.
     *
     * @param transcribedCalls a list of transcribed calls to be analyzed
     */

    public List<AnalyzedCall> startAnalysis(List<TranscribedCallInformation> transcribedCalls) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<?>> futures = new ArrayList<>();

        List<AnalyzedCall> analyzedCalls = new ArrayList<>();

        for (var call : transcribedCalls) {
            Future<?> future = executorService.submit(() -> {
                try {
                    AnalyzeResult analyzedCallResult = analyzer.getAnalyzeResult(call);
                    analyzedCalls.add(analyzer.buildJsonFile(analyzedCallResult, call));
                } catch (ExecutionException | InterruptedException e) {
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
                logger.error("An error occurred when reading prompt.txt: " + e.getMessage());
                throw new RuntimeException("Exception thrown in OpenAiAnalyzer, analyze " + e.getMessage());
            }
        }
        executorService.shutdown(); // Always remember to shutdown the executor service
        return analyzedCalls;
    }

}
