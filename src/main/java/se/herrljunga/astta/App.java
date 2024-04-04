package se.herrljunga.astta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.herrljunga.astta.analyze.MultiThreadAnalyzer;
import se.herrljunga.astta.analyze.OpenAIAnalyzer;
import se.herrljunga.astta.filehandler.BlobStorageHandler;
import se.herrljunga.astta.filehandler.StorageHandler;
import se.herrljunga.astta.keyvault.KeyVault;
import se.herrljunga.astta.speechtotext.BatchTranscriber;
import se.herrljunga.astta.utils.AnalyzedCall;
import se.herrljunga.astta.utils.Config;
import se.herrljunga.astta.utils.TranscribedCallInformation;
import se.herrljunga.astta.utils.Utils;

import java.io.IOException;
import java.util.List;


public class App {
    static StorageHandler reportBlobStorage = new BlobStorageHandler(KeyVault.getSecret(Config.blobStorageEndpoint),
            KeyVault.getSecret(Config.sasTokenSecretName),
            Config.textSaveContainerName);
    static StorageHandler powerBiBlobStorage = new BlobStorageHandler(KeyVault.getSecret(Config.blobStorageEndpoint),
            KeyVault.getSecret(Config.sasTokenSecretName),
            Config.powerBiContainerName);
    static StorageHandler tempBlobStorage = new BlobStorageHandler(KeyVault.getSecret(Config.blobStorageEndpoint),
            KeyVault.getSecret(Config.sasTokenSecretName),
            Config.tempContainerName);
    static StorageHandler audioSource = new BlobStorageHandler(KeyVault.getSecret(Config.blobStorageEndpoint),
            KeyVault.getSecret(Config.sasTokenSecretName),
            Config.audioSourceContainerName);
    static MultiThreadAnalyzer multiThreadAnalyzer = new MultiThreadAnalyzer(new OpenAIAnalyzer(KeyVault.getSecret(Config.openaiSecretName), KeyVault.getSecret(Config.openaiEndpoint), Config.openaiModel));
    static BatchTranscriber batchTranscriber = new BatchTranscriber();

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(App.class);
        logger.debug("Starting logger");
        try {
            // Transcribe:
            batchTranscriber.startTranscription();

            List<String> transcribedPaths = tempBlobStorage.fetchFile();

            List<String> filteredTranscribedPaths = Utils.extractReport(transcribedPaths, reportBlobStorage);

            List<TranscribedCallInformation> transcribedCallInformations = OpenAIAnalyzer.extractInformationFromTranscribedFiles(filteredTranscribedPaths);

            // Run the whole chain
            multiThreadAnalyzer.startAnalysis(transcribedCallInformations, powerBiBlobStorage, audioSource);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception occured: ", e);
        } finally {
            tempBlobStorage.deleteContainer();
            System.exit(0);
        }
    }
}
