package se.herrljunga.astta.filehandler;

import java.util.List;

public interface StorageHandler {
    List<byte[]> fetchByte();
    void saveByte(List<byte[]> dataList);
    List<String> fetchFile();
    void saveFile(String path);
    void deleteFile(String path);

}
