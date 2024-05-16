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
import se.herrljunga.astta.utils.ConfigLoader;
import se.herrljunga.astta.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
// TODO Separate to two classes, one for the entire blob, one for blob container

/**
 * The BlobStorageHandler class implements the StorageHandler interface.
 *
 * This class provides methods for interacting with Azure Blob Storage.
 * It includes functionality for fetching files from Blob Storage, saving files to Blob Storage,
 * deleting files from Blob Storage, and managing Blob Storage containers.
 *
 * The class is initialized with an endpoint and a Shared Access Signature (SAS) token for the Azure Blob Storage service.
 * It can also be initialized with a StorageSharedKeyCredential for authentication.
 */
public class BlobStorageHandler implements StorageHandler {
    private static Config config = ConfigLoader.loadConfig();
    BlobServiceClient blobServiceClient;
    BlobContainerClient blobContainerClient;
    List<String> blobFilePath = new ArrayList<>();
    Logger logger = LoggerFactory.getLogger(BlobStorageHandler.class);

    /**
     * Constructs a BlobStorageHandler object.
     *
     * This constructor initializes the BlobServiceClient with the provided endpoint and SAS token.
     * It also initializes the BlobContainerClient with the Blob Storage container name.
     * The BlobServiceClient is used to interact with the Azure Blob Storage service.
     * The BlobContainerClient is used to interact with a specific Blob Storage container.
     *
     * @param endpoint The endpoint URL of the Azure Blob Storage service.
     * @param sasToken The Shared Access Signature (SAS) token for accessing the Blob Storage.
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
     * Constructs a BlobStorageHandler object with a StorageSharedKeyCredential which is necessary when creating a new blob container within the application.
     *
     * This constructor initializes the BlobServiceClient with the provided endpoint, SAS token, and StorageSharedKeyCredential.
     * The BlobServiceClient is used to interact with the Azure Blob Storage service.
     *
     * @param endpoint The endpoint URL of the Azure Blob Storage service.
     * @param sasToken The Shared Access Signature (SAS) token for accessing the Blob Storage.
     * @param credential The StorageSharedKeyCredential used for authentication.
     */
    public BlobStorageHandler(String endpoint, String sasToken, StorageSharedKeyCredential credential) {
        this.blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .sasToken(sasToken)
                .credential(credential)
                .buildClient();

    }


    /**
     * Fetches WAV files from the Blob Storage container and saves them temporarily on the local filesystem.
     *
     * This method starts by logging the start of the fetching process and creating a fixed thread pool executor service.
     * It then iterates over each BlobItem in the Blob Storage container.
     * For each BlobItem, it submits a task to the executor service to fetch the file and save it locally.
     * The method waits for all tasks to complete before shutting down the executor service and logging the completion of the fetching process.
     * Note: This method assumes that the BlobContainerClient has been initialized with the appropriate Blob Storage container.
     *
     * @return A list of paths to the temporarily saved WAV files.
     * @throws RuntimeException if an error occurs during the fetching process or if an error occurs when waiting for tasks to complete.
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
                    logger.info("Fetching file: {}", blobName);
                    // blobName - Adding the same name as the file in Blob Storage
                    BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
                    blobFilePath.add(blobName);
                    blobClient.downloadToFile(config.utils.pathToTemp + Utils.removePathFromFilename(blobName));
                    paths.add(config.utils.pathToTemp + Utils.removePathFromFilename(blobName));
                    logger.info("Done fetching file: {}", Utils.removePathFromFilename(blobName));
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
                logger.error("An error occurred when waiting for tasks to complete: {}", e.getMessage());
                throw new RuntimeException("Exception thrown in OpenAiAnalyzer, analyze " + e.getMessage());
            }
        }
        executorService.shutdown(); // Always remember to shutdown the executor service
        logger.info("Done fetching files");
        return paths;
    }

    /**
     * Saves a single file to Azure Blob Storage.
     *
     * This method attempts to save a file to Azure Blob Storage.
     * It logs the start of the saving process and the name of the file being saved.
     * If an error occurs during the saving process, it logs the error and throws a RuntimeException.
     *
     * @param filePath The local file path of the file to be saved.
     * @throws RuntimeException if an error occurs during the saving process.
     */
    @Override
    public void saveSingleFileToStorage(String filePath) {
        logger.info("Saving to storage: {}", Utils.removePathFromFilename(filePath));
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(Utils.removePathFromFilename(filePath)); // Name of saved file
            blobClient.uploadFromFile(filePath, true);
        } catch (BlobStorageException | StorageErrorException e) {
            System.err.println("An error saving files to blob");
            throw new RuntimeException("Exception thrown in BlobStorageHandler, saveToStorage " + e.getMessage());
        }
        logger.info("Done saving to storage: {}", filePath);
    }

    /**
     * Saves a list of AnalyzedCall objects to Azure Blob Storage.
     *
     * This method iterates over a list of AnalyzedCall objects and saves each one to Azure Blob Storage.
     * It does this by calling the saveSingleFileToStorage method with the save path of each AnalyzedCall object.
     *
     * @param analyzedCalls The list of AnalyzedCall objects to be saved.
     */
    @Override
    public void saveToStorage(List<AnalyzedCall> analyzedCalls) {
        for (var analyzedCall : analyzedCalls) {
            saveSingleFileToStorage(analyzedCall.savePath());
        }
    }

    /**
     * Deletes the Blob Storage container.
     *
     * This method attempts to delete the Blob Storage container and logs a warning message indicating the start of the deletion process.
     * The success of the deletion operation is also logged.
     * Note: This method assumes that the BlobContainerClient has been initialized with the appropriate Blob Storage container.
     */
    public void deleteContainer() {
        logger.warn("Deleting container {}, success: {}", blobContainerClient.getBlobContainerName(), blobContainerClient.deleteIfExists());
    }

    /**
     * Creates a temporary container in Azure Blob Storage.
     *
     * This method attempts to create a new Blob Storage container with the specified name.
     * If the container does not exist, it is created and a reference to it is returned.
     * If the container already exists, a reference to the existing container is returned.
     * The method also generates a Shared Access Signature (SAS) token with write rights for the new blob container.
     * The method pauses for 500 milliseconds after creating the container to allow for the SAS token to be generated.
     *
     * @param containerName The name of the Blob Storage container to be created.
     * @return A BlobContainerClient object representing the newly created or existing Blob Storage container.
     * @throws RuntimeException if a BlobStorageException occurs when creating the container or if the thread is interrupted during the pause.
     */
    public BlobContainerClient createTempContainer(String containerName) {
        BlobContainerClient newContainerClient = null;
        try {
            var response = blobServiceClient.createBlobContainerIfNotExistsWithResponse(containerName, null, Context.NONE);
            newContainerClient = response.getValue();
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
     * The OpenAIAnalyzer class provides functionality to analyze text using the OpenAI API.
     * It includes methods to analyze transcribed text, extract information from transcribed files,
     * build a JSON file from the result of an analyzed call, and get the analysis result of a transcribed call.
     *
     * The class is initialized with an API key, endpoint, and deployment or model ID for the OpenAI service.
     * It uses a custom HttpClient with a modified response timeout and a RetryPolicy for handling retries.
     */
    @Override
    public void deleteFromStorage(String fileToDeletePath) {
        logger.info("Deleting file from storage: {}", Utils.removePathFromFilename(fileToDeletePath));
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(Utils.removePathFromFilename(fileToDeletePath));
            blobClient.deleteIfExists();
        } catch (BlobStorageException | StorageErrorException e) {
            logger.info("Error deleting files from blob: ", e);
            throw new RuntimeException("Exception thrown in BlobStorageHandler, deleteFromStorage " + e.getMessage());
        }
        logger.info("Done deleting from storage: {}", Utils.removePathFromFilename(fileToDeletePath));
    }

    public List<String> getBlobFilePath() {
        if (blobFilePath.isEmpty()) {
            fetchFile();
        }
        return blobFilePath;
    }
}
