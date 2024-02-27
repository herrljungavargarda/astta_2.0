package se.herrljunga.astta.filehandler;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import se.herrljunga.astta.utils.Config;
import se.herrljunga.astta.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the FetchSave interface for retrieving audio files from Azure Blob Storage.
 */
public class BlobStorageHandler implements StorageHandler {
    BlobServiceClient blobServiceClient;
    BlobContainerClient blobContainerClient;

    /**
     * Constructs a FetchSaveImpl object.
     *
     * @param endpoint          The endpoint URL of the Azure Blob Storage service.
     * @param sasToken          The Shared Access Signature (SAS) token for accessing the Blob Storage.
     * @param blobContainerName The name of the Blob Storage container.
     */
    public BlobStorageHandler(String endpoint, String sasToken, String blobContainerName) {
        this.blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .sasToken(sasToken) //TODO: Byt ut mot miljövariabel
                .buildClient();

        this.blobContainerClient = blobServiceClient.getBlobContainerClient(blobContainerName);
    }

    /**
     * Fetches audio files from the Blob Storage container.
     *
     * @return A list of byte arrays representing the fetched audio files.
     */
    @Override
    public List<byte[]> fetchByte() {
        List<byte[]> data = new ArrayList<>();
        for (BlobItem blobItem : blobContainerClient.listBlobs()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // Retrieve file title
            String blobName = blobItem.getName();
            BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
            blobClient.downloadStream(outputStream);
            data.add(outputStream.toByteArray());
        }
        return data;
    }

    @Override
    public void saveByte(List<byte[]> dataList) {
        //TODO: Behöver kollas över innan impl
//        for (int i = 0; i < dataList.size(); i++) {
//            byte[] data = dataList.get(i);
//            File outputFile = new File("output" + i + ".wav");
//            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
//                fos.write(data);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    /**
     * Clears temp directory and deletes it if exists
     * Create a new temp directory
     * Fetches all data from blob storage
     * Saves all files to temp file
     */


    /**
     * Fetches WAV files from the Blob Storage container and saves them temporarily on the local filesystem.
     * Note: This method assumes that the BlobContainerClient has been initialized with the appropriate Blob Storage container.
     *
     * @return A list of paths to the temporarily saved WAV files.
     */
    @Override
    public List<String> fetchFile() {
        List<String> paths = new ArrayList<>();
        Utils.createTempFile();
        for (BlobItem blobItem : blobContainerClient.listBlobs()) {
            // Retrieve file title
            String blobName = blobItem.getName();
            System.out.println("Fetching: " + blobName);
            // blobName - Adding the same name as the file in Blob Storage
            BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
            blobClient.downloadToFile(Config.pathToTemp + blobName);
            paths.add(Config.pathToTemp + blobName);
        }
        return paths;
    }

    /**
     * Saves a file to Azure Blob Storage
     *
     * @param filePath The local file path of the file to be saved.
     */
    @Override
    public void saveToStorage(String filePath) {
        BlobClient blobClient = blobContainerClient.getBlobClient(Utils.removePathFromFilename(filePath)); // Name of saved file
        if (!blobClient.exists()) {
            //System.out.println(":(");
        }
        blobClient.uploadFromFile(filePath, true);
        if (blobClient.exists()) {
            //System.out.println(":)");
        }

    }

    @Override
    public void deleteFromStorage(String fileToDeletePath) {
        BlobClient blobClient = blobContainerClient.getBlobClient(Utils.removePathFromFilename(fileToDeletePath));
        blobClient.deleteIfExists();
    }

}
