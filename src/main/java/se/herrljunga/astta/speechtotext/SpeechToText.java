package se.herrljunga.astta.speechtotext;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface SpeechToText {
    String speechToText(byte[] audioFile);
    String[] speechToText(String path) throws InterruptedException, ExecutionException;
    void close();
}
