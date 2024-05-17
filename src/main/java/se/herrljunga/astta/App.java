package se.herrljunga.astta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.herrljunga.astta.analyze.MultiThreadAnalyzer;
import se.herrljunga.astta.analyze.OpenAIAnalyzer;
import se.herrljunga.astta.emailsender.EmailSender;
import se.herrljunga.astta.filehandler.BlobStorageHandler;
import se.herrljunga.astta.filehandler.StorageHandler;
import se.herrljunga.astta.keyvault.KeyVault;
import se.herrljunga.astta.speechtotext.BatchTranscriber;
import se.herrljunga.astta.utils.*;

import javax.mail.MessagingException;
import java.io.File;
import java.util.List;


public class App {
    private static final Config config = ConfigLoader.loadConfig();
    static StorageHandler reportBlobStorage = new BlobStorageHandler(KeyVault.getSecret(config.blobStorage.endpoint),
            KeyVault.getSecret(config.blobStorage.sasTokenSecretName),
            config.blobStorage.reportSaveContainerName);
    static StorageHandler powerBiBlobStorage = new BlobStorageHandler(KeyVault.getSecret(config.blobStorage.endpoint),
            KeyVault.getSecret(config.blobStorage.sasTokenSecretName),
            config.blobStorage.powerBiContainerName);
    static StorageHandler tempBlobStorage = new BlobStorageHandler(KeyVault.getSecret(config.blobStorage.endpoint),
            KeyVault.getSecret(config.blobStorage.sasTokenSecretName),
            config.blobStorage.tempContainerName);
    static StorageHandler audioSource = new BlobStorageHandler(KeyVault.getSecret(config.blobStorage.endpoint),
            KeyVault.getSecret(config.blobStorage.sasTokenSecretName),
            config.blobStorage.audioSourceContainerName);
    static MultiThreadAnalyzer multiThreadAnalyzer = new MultiThreadAnalyzer(new OpenAIAnalyzer(KeyVault.getSecret(config.openAI.secretName), KeyVault.getSecret(config.openAI.endpoint), config.openAI.model));
    static BatchTranscriber batchTranscriber = new BatchTranscriber();
    static EmailSender emailSender = new EmailSender(KeyVault.getSecret(config.emailSender.smtpHost), KeyVault.getSecret(config.emailSender.smtpPort), KeyVault.getSecret(config.emailSender.smtpUsername), KeyVault.getSecret(config.emailSender.smtpPassword));

    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(App.class);
        logger.debug("Starting logger");
        try {
            // Transcribe:
            batchTranscriber.startTranscription();

            List<String> transcribedPaths = tempBlobStorage.fetchFile();

            List<String> filteredTranscribedPaths = Utils.extractReport(transcribedPaths, reportBlobStorage);

            List<TranscribedCallInformation> transcribedCallInformations = OpenAIAnalyzer.extractInformationFromTranscribedFiles(filteredTranscribedPaths);

            // Run the whole analyze chain
            multiThreadAnalyzer.startAnalysis(transcribedCallInformations, powerBiBlobStorage, audioSource);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception occured: ", e);
            try {
                emailSender.sendEmail(KeyVault.getSecret(config.emailSender.smtpToAddress), "Astta error!", e.getMessage());
            }
            catch (MessagingException ex) {
                logger.error("Exception occured: ", ex);
            }
        } finally {
            Utils.deleteFolderIfExists(new File(config.utils.pathToTemp));
            tempBlobStorage.deleteContainer();
            System.exit(0);
        }
    }
}
