package se.herrljunga.astta.utils;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;

import java.time.OffsetDateTime;

public class GenerateSasToken {
    static String tempContainerSasToken;
    public static void generateSasToken(BlobContainerClient newContainerClient) {
        BlobContainerSasPermission permissions = new BlobContainerSasPermission().setWritePermission(true);
        OffsetDateTime expiryTime = OffsetDateTime.now().plusDays(1);
        BlobServiceSasSignatureValues sasSignatureValues = new BlobServiceSasSignatureValues(expiryTime, permissions);
        tempContainerSasToken = newContainerClient.generateSas(sasSignatureValues);
    }

    public static String getTempContainerSasToken() {
        return tempContainerSasToken;
    }
}
