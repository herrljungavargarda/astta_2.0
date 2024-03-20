package se.herrljunga.astta.speechtotext;

import se.herrljunga.astta.utils.TranscribedCallInformation;

import java.util.concurrent.ExecutionException;

public interface SpeechToText {
    TranscribedCallInformation speechToText(String path) throws InterruptedException, ExecutionException;
    void close();
}
