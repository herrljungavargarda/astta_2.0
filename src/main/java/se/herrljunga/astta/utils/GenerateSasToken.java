package se.herrljunga.astta.utils;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;

import java.time.OffsetDateTime;

/**
 * The GenerateSasToken class.
 *
 * This class provides a method to generate a Shared Access Signature (SAS) token for a BlobContainerClient.
 * The SAS token provides secure delegated access to resources in your storage account without sharing your account keys.
 * This is useful for scenarios where you want to provide a client with temporary access to resources.
 */
public class GenerateSasToken {

    /**
     * Generates a SAS token for the specified BlobContainerClient.
     *
     * This method creates a BlobContainerSasPermission object with write permission, sets the expiry time to 1 day from now,
     * and creates a BlobServiceSasSignatureValues object with these values.
     * It then calls the generateSas method on the BlobContainerClient with the BlobServiceSasSignatureValues to generate the SAS token.
     *
     * @param newContainerClient The BlobContainerClient for which to generate the SAS token.
     * @return The SAS token as a string.
     */
    public static String generateSasToken(BlobContainerClient newContainerClient) {
        BlobContainerSasPermission permissions = new BlobContainerSasPermission().setWritePermission(true);
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(1);
        BlobServiceSasSignatureValues sasSignatureValues = new BlobServiceSasSignatureValues(expiryTime, permissions);
        return newContainerClient.generateSas(sasSignatureValues);
    }
}