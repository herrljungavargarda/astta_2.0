package se.herrljunga.astta.keyvault;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;

public class KeyVault {
    private static final String keyVaultName = System.getenv("KEY_VAULT_NAME");
    private static final String keyVaultUri = "https://" + keyVaultName + ".vault.azure.net";
    private static SecretClient secretClient;

    public static String getSecret(String keySecretName) {
        if (secretClient == null) {
            secretClient = new SecretClientBuilder()
                    .vaultUrl(keyVaultUri)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
        }
        return secretClient.getSecret(keySecretName).getValue();
    }
}
