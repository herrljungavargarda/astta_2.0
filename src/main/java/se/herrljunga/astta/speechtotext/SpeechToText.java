package se.herrljunga.astta.speechtotext;

import se.herrljunga.astta.utils.TranscribedTextAndLanguage;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface SpeechToText {
    String speechToText(byte[] audioFile);
    TranscribedTextAndLanguage speechToText(String path) throws InterruptedException, ExecutionException;
    void close();
}
