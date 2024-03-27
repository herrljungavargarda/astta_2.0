package se.herrljunga.astta.filehandler;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.implementation.models.StorageErrorException;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.herrljunga.astta.utils.AnalyzedCall;
import se.herrljunga.astta.utils.Config;
import se.herrljunga.astta.utils.GenerateSasToken;
import se.herrljunga.astta.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
// TODO Separate to two classes, one for the entire blob, one for blob container

/**
 * Implementation of the FetchSave interface for retrieving audio files from Azure Blob Storage.
 */
public class BlobStorageHandler implements StorageHandler {
    BlobServiceClient blobServiceClient;
    BlobContainerClient blobContainerClient;
    List<String> blobFilePath = new ArrayList<>();
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


    public BlobStorageHandler(String endpoint, String sasToken, StorageSharedKeyCredential credential) {
        this.blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .sasToken(sasToken)
                .credential(credential)
                .buildClient();

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
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        Utils.createTempDirectory();
        for (BlobItem blobItem : blobContainerClient.listBlobs()) {
        Future<?> future = executorService.submit(() -> {
            try {
                    // Retrieve file title
                    String blobName = blobItem.getName();
                    logger.info("Fetching file: " + blobName);
                    // blobName - Adding the same name as the file in Blob Storage
                    BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
                    blobFilePath.add(blobName);
                    blobClient.downloadToFile(Config.pathToTemp + Utils.removePathFromFilename(blobName));
                    paths.add(Config.pathToTemp + Utils.removePathFromFilename(blobName));
                    logger.info("Done fetching file: " + Utils.removePathFromFilename(blobName));
                } catch (BlobStorageException | StorageErrorException e) {
                    System.err.println("An error fetching files from blob");
                    throw new RuntimeException("Exception thrown in BlobStorageHandler, fetchFile " + e.getMessage());
            }
        });
        futures.add(future);
    }
        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("An error occurred when waiting for tasks to complete: " + e.getMessage());
                throw new RuntimeException("Exception thrown in OpenAiAnalyzer, analyze " + e.getMessage());
            }
        }
        executorService.shutdown(); // Always remember to shutdown the executor service
        logger.info("Done fetching files");
        return paths;
    }
    /**
     * Saves a file to Azure Blob Storage
     *
     * @param filePath The local file path of the file to be saved.
     */
    @Override
    public void saveSingleFileToStorage(String filePath) {
        logger.info("Saving to storage: " + Utils.removePathFromFilename(filePath));
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
     * Saves a file to Azure Blob Storage
     *
     * @param analyzedCalls The local file path of the file to be saved.
     */
    @Override
    public void saveToStorage(List<AnalyzedCall> analyzedCalls) {
        for (var analyzedCall : analyzedCalls) {
            saveSingleFileToStorage(analyzedCall.savePath());
        }
    }
    public void deleteContainer() {
        blobContainerClient.deleteIfExists();
    }

    public BlobContainerClient createTempContainer(String containerName) {
        BlobContainerClient newContainerClient = null;
        try {
            var response = blobServiceClient.createBlobContainerIfNotExistsWithResponse(containerName, null, Context.NONE);
            newContainerClient= response.getValue();
            // Generate a SAS token with write rights for the new blob container
            Thread.sleep(500);
        } catch (BlobStorageException e) {
            System.err.println("Error creating new container: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        this.blobContainerClient = newContainerClient;
        return newContainerClient;
    }

    /**
     * Deletes specified file from Blob Storage
     *
     * @param fileToDeletePath The local file path of the file to be deleted
     */

    @Override
    public void deleteFromStorage(String fileToDeletePath) {
        logger.info("Deleting file from storage: " + Utils.removePathFromFilename(fileToDeletePath));
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(Utils.removePathFromFilename(fileToDeletePath));
            blobClient.deleteIfExists();
        } catch (BlobStorageException | StorageErrorException e) {
            logger.info("Error deleting files from blob: ", e);
            throw new RuntimeException("Exception thrown in BlobStorageHandler, deleteFromStorage " + e.getMessage());
        }
        logger.info("Done deleting from storage: " + Utils.removePathFromFilename(fileToDeletePath));
    }

    public List<String> getBlobFilePath() {
        if (blobFilePath.isEmpty()) {
            fetchFile();
        }
        return blobFilePath;
    }
}
