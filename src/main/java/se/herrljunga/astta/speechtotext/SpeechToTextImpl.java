package se.herrljunga.astta.speechtotext;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.herrljunga.astta.utils.TranscribedCallInformation;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of the SpeechToText interface for converting speech to text using Azure Cognitive Services.
 */
@Deprecated
public class SpeechToTextImpl implements SpeechToText {
    SpeechConfig speechConfig;
    private Logger logger = LoggerFactory.getLogger(SpeechToTextImpl.class);
    AutoDetectSourceLanguageConfig autoDetectSourceLanguageConfig;
    AudioConfig audioConfig;
    ConversationTranscriber conversationTranscriber;
    Semaphore stopRecognitionSemaphore;
    StringBuilder sb;

    /**
     * Constructs a SpeechToTextImpl object.
     *
     * @param speechKey                 The subscription key for accessing Azure Speech service.
     * @param speechRegion              The region where the Azure Speech service is hosted.
     * @param speechRecognitionLanguage The language in which the speech is spoken.
     */
    public SpeechToTextImpl(String speechKey, String speechRegion, String speechRecognitionLanguage, AutoDetectSourceLanguageConfig autoDetectSourceLanguageConfig) {
        this.speechConfig = SpeechConfig.fromSubscription(speechKey, speechRegion);
        this.speechConfig.setSpeechRecognitionLanguage(speechRecognitionLanguage);
        this.autoDetectSourceLanguageConfig = autoDetectSourceLanguageConfig;
    }

    /**
     * Transcribes speech from WAV files located at the specified paths.
     * Each WAV file is transcribed individually.
     *
     * @param path A list of paths to WAV files to be transcribed.
     * @return A list of transcribed text results, each corresponding to a WAV file.
     * @throws InterruptedException If the current thread is interrupted while waiting for the transcription to complete.
     * @throws ExecutionException   If an error occurs during the execution of the transcription process.
     */
    @Override
    public TranscribedCallInformation speechToText(String path) {
        logger.info("Starting speech to text conversion for file at path: " + path);
        try {
            TranscribedCallInformation transcribedCallInformation = new TranscribedCallInformation();
            sb = new StringBuilder();
            stopRecognitionSemaphore = new Semaphore(0);
            audioConfig = AudioConfig.fromWavFileInput(path);
            conversationTranscriber = new ConversationTranscriber(speechConfig, autoDetectSourceLanguageConfig, audioConfig);
            AtomicBoolean languageDetected = new AtomicBoolean(false); // Flag to indicate if transcribedTextAndLanguage detection has been executed

            // Subscribes to events.
            conversationTranscriber.transcribed.addEventListener((s, e) -> {
                if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                    sb.append(e.getResult().getText()).append(" Speaker ID=").append(e.getResult().getSpeakerId()).append("\t");

                    if (!languageDetected.get()) { // Check if transcribedTextAndLanguage detection has been executed
                        AutoDetectSourceLanguageResult autoDetectSourceLanguageResult = AutoDetectSourceLanguageResult.fromResult(e.getResult());
                        String detectedLanguage = autoDetectSourceLanguageResult.getLanguage();
                        //transcribedCallInformation.setLanguage(detectedLanguage);

                        languageDetected.set(true); // Set the flag to true to indicate transcribedTextAndLanguage detection has been executed
                    }
                } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                    logger.info("NOMATCH: Speech could not be transcribed.");
                }
            });

            conversationTranscriber.canceled.addEventListener((s, e) -> {
                logger.info("Done transcribing: " + e.getReason());
                if (e.getReason() == CancellationReason.Error) {
                    logger.info("CANCELED: ErrorCode=" + e.getErrorCode());
                    logger.info("CANCELED: ErrorDetails=" + e.getErrorDetails());
                    logger.info("CANCELED: Did you update the subscription info?");
                }
                stopRecognitionSemaphore.release();
            });

            conversationTranscriber.sessionStarted.addEventListener((s, e) -> {
                logger.info("Starting transcription.");
            });

            conversationTranscriber.startTranscribingAsync().get();
            // Waits for completion.
            stopRecognitionSemaphore.acquire();
            conversationTranscriber.stopTranscribingAsync().get();

            transcribedCallInformation.setTranscribedText(sb.toString());

            logger.info("Completed speech to text conversion for file at path: " + path);
            return transcribedCallInformation;
        }
        catch (InterruptedException | ExecutionException e) {
            logger.error("An error occurred when transcribing audio file" + e);
            throw new RuntimeException("Exception thrown in SpeechToTextImpl, speechToText " + e.getMessage());
        }
    }

    /**
     * Closes the resources associated with the SpeechService.
     * This method releases all the resources used by the SpeechService, including closing the speech configuration, auto-detect source language configuration, audio configuration, and stopping the conversation transcriber.
     * After calling this method, the SpeechService instance becomes unusable.
     */


    public void close() {
        logger.info("Closing all active resources");
        speechConfig.close();
        autoDetectSourceLanguageConfig.close();
        audioConfig.close();

        conversationTranscriber.close();
        stopRecognitionSemaphore.release();

    }

}
