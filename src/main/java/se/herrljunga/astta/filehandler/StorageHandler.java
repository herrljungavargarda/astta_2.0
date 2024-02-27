package se.herrljunga.astta.filehandler;

import java.util.List;

public interface StorageHandler {
    List<byte[]> fetchByte();
    void saveByte(List<byte[]> dataList);
    List<String> fetchFile();
    void saveToStorage(String path);
    void deleteFromStorage(String path);

}
