package se.herrljunga.astta.filehandler;

import com.azure.storage.blob.BlobContainerClient;
import se.herrljunga.astta.utils.AnalyzedCall;

import java.util.List;

public interface StorageHandler {
    List<String> fetchFile();
    void saveToStorage(List<AnalyzedCall> analyzedCalls);
    void saveSingleFileToStorage(String filePath);
    void deleteFromStorage(String path);
    public List<String> getBlobFilePath();
    public void deleteContainer();
    public BlobContainerClient createTempContainer(String tempContainer);
}
