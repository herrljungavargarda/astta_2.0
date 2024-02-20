package se.herrljunga.astta.speechtotext;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import com.microsoft.cognitiveservices.speech.transcription.ConversationTranscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of the SpeechToText interface for converting speech to text using Azure Cognitive Services.
 */
public class SpeechToTextImpl implements SpeechToText {
    SpeechConfig speechConfig;
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
     * Converts speech data represented as a byte array to text. Input one file at a time.
     *
     * @param audioFile The audio data to be transcribed, provided as a byte array.
     * @return The transcribed text.
     */
    @Deprecated
    @Override
    public String speechToText(byte[] audioFile) {
        try (PushAudioInputStream pushStream = AudioInputStream.createPushStream();
            AudioConfig audioConfig = AudioConfig.fromStreamInput(pushStream)) {
            SpeechRecognizer speechRecognizer = new SpeechRecognizer(speechConfig, autoDetectSourceLanguageConfig, audioConfig);
            {
                pushStream.write(audioFile);
                pushStream.close();

                Future<SpeechRecognitionResult> task = speechRecognizer.recognizeOnceAsync();
                SpeechRecognitionResult speechRecognitionResult = task.get();
                if (speechRecognitionResult.getReason() == ResultReason.RecognizedSpeech) {
                    System.out.println("RECOGNIZED: Text=" + speechRecognitionResult.getText());
                    speechRecognizer.close();
                    return speechRecognitionResult.getText();
                } else if (speechRecognitionResult.getReason() == ResultReason.NoMatch) {
                    System.out.println("NOMATCH: Speech could not be recognized.");
                } else if (speechRecognitionResult.getReason() == ResultReason.Canceled) {
                    CancellationDetails cancellation = CancellationDetails.fromResult(speechRecognitionResult);
                    System.out.println("CANCELED: Reason=" + cancellation.getReason());

                    if (cancellation.getReason() == CancellationReason.Error) {
                        System.out.println("CANCELED: ErrorCode=" + cancellation.getErrorCode());
                        System.out.println("CANCELED: ErrorDetails=" + cancellation.getErrorDetails());
                        System.out.println("CANCELED: Did you set the speech resource key and region values?");
                    }
                }

            }
            speechRecognizer.close();
            return "Noting to read";

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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
    public String[] speechToText(String path) throws InterruptedException, ExecutionException {
        String[] transcribedText = new String[2];
        sb = new StringBuilder();
        stopRecognitionSemaphore = new Semaphore(0);
        audioConfig = AudioConfig.fromWavFileInput(path);
        conversationTranscriber = new ConversationTranscriber(speechConfig, autoDetectSourceLanguageConfig, audioConfig);
        AtomicBoolean languageDetected = new AtomicBoolean(false); // Flag to indicate if language detection has been executed

        // Subscribes to events.
        conversationTranscriber.transcribed.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                System.out.println("TRANSCRIBED: Text=" + e.getResult().getText() + " Speaker ID=" + e.getResult().getSpeakerId());
                sb.append(e.getResult().getText()).append(" Speaker ID=").append(e.getResult().getSpeakerId()).append("\t");

                if (!languageDetected.get()) { // Check if language detection has been executed
                    AutoDetectSourceLanguageResult autoDetectSourceLanguageResult = AutoDetectSourceLanguageResult.fromResult(e.getResult());
                    String detectedLanguage = autoDetectSourceLanguageResult.getLanguage();
                    transcribedText[1] = detectedLanguage;
                    System.out.println("Detected language: " + detectedLanguage);
                    languageDetected.set(true); // Set the flag to true to indicate language detection has been executed
                }
            } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                System.out.println("NOMATCH: Speech could not be transcribed.");
            }
        });

        conversationTranscriber.canceled.addEventListener((s, e) -> {
            System.out.println("CANCELED: Reason=" + e.getReason());
            if (e.getReason() == CancellationReason.Error) {
                System.out.println("CANCELED: ErrorCode=" + e.getErrorCode());
                System.out.println("CANCELED: ErrorDetails=" + e.getErrorDetails());
                System.out.println("CANCELED: Did you update the subscription info?");
            }
            stopRecognitionSemaphore.release();
        });

        conversationTranscriber.sessionStarted.addEventListener((s, e) -> {
            System.out.println("\n    Session started event.");
        });

        conversationTranscriber.startTranscribingAsync().get();
        // Waits for completion.
        stopRecognitionSemaphore.acquire();
        conversationTranscriber.stopTranscribingAsync().get();
        transcribedText[0] = sb.toString();



        return transcribedText;
    }

    public void close(){
        System.out.println("CLOSING");
        speechConfig.close();
        autoDetectSourceLanguageConfig.close();
        audioConfig.close();

        conversationTranscriber.close();
        stopRecognitionSemaphore.release();

    }

}
