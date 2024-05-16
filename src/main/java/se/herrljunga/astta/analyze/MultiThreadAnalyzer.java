package se.herrljunga.astta.analyze;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.herrljunga.astta.filehandler.StorageHandler;
import se.herrljunga.astta.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The MultiThreadAnalyzer class.
 * <p>
 * This class provides methods for multi-threaded analysis of transcribed calls.
 * It uses an instance of OpenAIAnalyzer to perform the analysis.
 * The analysis is performed in a separate thread for each transcribed call.
 */
public class MultiThreadAnalyzer {
    private static Config config = ConfigLoader.loadConfig();
    private final OpenAIAnalyzer analyzer;
    private final Logger logger = LoggerFactory.getLogger(OpenAIAnalyzer.class);

    /**
     * Constructs a new MultiThreadAnalyzer instance with the specified OpenAIAnalyzer.
     *
     * @param analyzer The OpenAIAnalyzer to be used for analysis.
     */
    public MultiThreadAnalyzer(OpenAIAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Starts multi-threaded analysis of transcribed calls.
     * <p>
     * This method uses an ExecutorService to manage threads. Each transcribed call is analyzed in a separate thread.
     * The method waits for all threads to complete before it returns.
     *
     * @param transcribedCalls   a list of transcribed calls to be analyzed
     * @param powerBiBlobStorage the StorageHandler for Power BI Blob Storage
     * @param audioSource        the StorageHandler for the audio source
     * @return a list of analyzed calls
     */
    public List<AnalyzedCall> startAnalysis(List<TranscribedCallInformation> transcribedCalls, StorageHandler powerBiBlobStorage, StorageHandler audioSource) {
        ExecutorService executorService = Executors.newFixedThreadPool(config.maxThreadsForAnalysis);
        List<Future<?>> futures = new ArrayList<>();

        List<AnalyzedCall> analyzedCalls = new ArrayList<>();

        for (var call : transcribedCalls) {
            Future<?> future = executorService.submit(() -> {
                try {
                    AnalyzeResult analyzedCallResult = analyzer.getAnalyzeResult(call);
                    AnalyzedCall analyzedCall = analyzer.buildJsonFile(analyzedCallResult, call);
                    analyzedCalls.add(analyzedCall);
                    powerBiBlobStorage.saveSingleFileToStorage((analyzedCall).savePath());
                    audioSource.deleteFromStorage(Utils.getFileName((analyzedCall).savePath()));
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