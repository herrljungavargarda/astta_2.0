package se.herrljunga.astta.filehandler;

import java.util.List;

public interface StorageHandler {
    List<String> fetchFile();
    void saveToStorage(String path);
    void deleteFromStorage(String path);

}
