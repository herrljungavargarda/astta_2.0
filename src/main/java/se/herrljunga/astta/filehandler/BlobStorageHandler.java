package se.herrljunga.astta.filehandler;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.implementation.models.StorageErrorException;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.herrljunga.astta.utils.Config;
import se.herrljunga.astta.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the FetchSave interface for retrieving audio files from Azure Blob Storage.
 */
public class BlobStorageHandler implements StorageHandler {
    BlobServiceClient blobServiceClient;
    BlobContainerClient blobContainerClient;

    Logger logger = LoggerFactory.getLogger(BlobStorageHandler.class);

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
                .sasToken(sasToken)
                .buildClient();

        this.blobContainerClient = blobServiceClient.getBlobContainerClient(blobContainerName);
    }


    /**
     * Fetches WAV files from the Blob Storage container and saves them temporarily on the local filesystem.
     * Note: This method assumes that the BlobContainerClient has been initialized with the appropriate Blob Storage container.
     *
     * @return A list of paths to the temporarily saved WAV files.
     */
    @Override
    public List<String> fetchFile() {
        logger.info("Fetching files");
        try {
            List<String> paths = new ArrayList<>();
            Utils.createTempDirectory();
            for (BlobItem blobItem : blobContainerClient.listBlobs()) {
                // Retrieve file title
                String blobName = blobItem.getName();
                logger.info("Fetching file: " + blobName);
                // blobName - Adding the same name as the file in Blob Storage
                BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
                blobClient.downloadToFile(Config.pathToTemp + blobName);
                paths.add(Config.pathToTemp + blobName);
            }
            logger.info("Done fetching files");

            return paths;
        } catch (BlobStorageException | StorageErrorException e) {
            System.err.println("An error fetching files from blob");
            throw new RuntimeException("Exception thrown in BlobStorageHandler, fetchFile " + e.getMessage());
        }
    }

    /**
     * Saves a file to Azure Blob Storage
     *
     * @param filePath The local file path of the file to be saved.
     */
    @Override
    public void saveToStorage(String filePath) {
        logger.info("Saving to storage: " + filePath);
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(Utils.removePathFromFilename(filePath)); // Name of saved file
            blobClient.uploadFromFile(filePath, true);
        } catch (BlobStorageException | StorageErrorException e) {
            System.err.println("An error saving files to blob");
            throw new RuntimeException("Exception thrown in BlobStorageHandler, saveToStorage " + e.getMessage());
        }
        logger.info("Done saving to storage: " + filePath);
    }

    /**
     * Deletes specified file from Blob Storage
     *
     * @param fileToDeletePath The local file path of the file to be deleted
     */

    @Override
    public void deleteFromStorage(String fileToDeletePath) {
        logger.info("Deleting file from storage: " + fileToDeletePath);
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(Utils.removePathFromFilename(fileToDeletePath));
            blobClient.deleteIfExists();
        }
        catch (BlobStorageException | StorageErrorException e) {
            logger.info("Error deleting files from blob: ", e);
            throw new RuntimeException("Exception thrown in BlobStorageHandler, deleteFromStorage " + e.getMessage());
        }
        logger.info("Done deleting from storage: " + fileToDeletePath);
    }

}
