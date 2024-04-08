package se.herrljunga.astta.analyze;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.herrljunga.astta.filehandler.StorageHandler;
import se.herrljunga.astta.utils.AnalyzedCall;
import se.herrljunga.astta.utils.Config;
import se.herrljunga.astta.utils.TranscribedCallInformation;
import se.herrljunga.astta.utils.Utils;

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

    public List<AnalyzedCall> startAnalysis(List<TranscribedCallInformation> transcribedCalls, StorageHandler powerBiBlobStorage, StorageHandler audioSource) {
        ExecutorService executorService = Executors.newFixedThreadPool(Config.maxThreadsForAnalysis);
        List<Future<?>> futures = new ArrayList<>();

        List<AnalyzedCall> analyzedCalls = new ArrayList<>();

        for (var call : transcribedCalls) {
            Future<?> future = executorService.submit(() -> {
                try {
                    AnalyzeResult analyzedCallResult = analyzer.getAnalyzeResult(call);

                    analyzedCalls.add(analyzer.buildJsonFile(analyzedCallResult, call));
                    powerBiBlobStorage.saveSingleFileToStorage(analyzer.buildJsonFile(analyzedCallResult, call).savePath());
                    //audioSource.deleteFromStorage(Utils.getFileName(analyzer.buildJsonFile(analyzedCallResult, call).savePath()));
                } catch (Exception e) {
                    logger.error("An error occurred when analysing the file: {}\n{}", Utils.removePathFromFilename(call.getPath()), e.getMessage());
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
                logger.error("An error occurred when waiting for tasks to complete: {}", e.getMessage());
                throw new RuntimeException("Exception thrown in OpenAiAnalyzer, analyze " + e.getMessage());
            }
        }
        executorService.shutdown(); // Always remember to shut down the executor service
        return analyzedCalls;
    }

}
