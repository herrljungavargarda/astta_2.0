package se.herrljunga.astta;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import se.herrljunga.astta.filehandler.StorageHandler;
import se.herrljunga.astta.filehandler.BlobStorageHandler;
import se.herrljunga.astta.keyvault.KeyVault;
import se.herrljunga.astta.utils.Config;
import se.herrljunga.astta.utils.ConfigLoader;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FetchSaveTest {
    Config config = ConfigLoader.loadConfig();
    @Disabled
    @Test
    public void testFetch() {
        StorageHandler fetchSave = new BlobStorageHandler(KeyVault.getSecret(config.blobStorage.endpoint),
                KeyVault.getSecret(config.blobStorage.sasTokenSecretName),
                config.blobStorage.audioSourceContainerName);
        List<byte[]> result = null;
        assertThat(result).isNotEmpty();
    }
}