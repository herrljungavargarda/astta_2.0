package se.herrljunga.astta.filehandler;

import java.util.List;

public interface StorageHandler {
    List<String> fetchFile();
    void saveToStorage(String path);
    void deleteFromStorage(String path);
    public List<String> getBlobFilePath();
    public void deleteContainer();
    public void createTempContainer(String tempContainer);
}
