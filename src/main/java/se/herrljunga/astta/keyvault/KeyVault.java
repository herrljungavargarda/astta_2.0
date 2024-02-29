package se.herrljunga.astta.keyvault;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;

/**
 * This class provides methods to interact with Azure Key Vault to retrieve secrets.
 */
public class KeyVault {
    private static final String keyVaultName = System.getenv("KEY_VAULT_NAME");

    /**
     * The URI of the Key Vault.
     */
    private static final String keyVaultUri = "https://" + keyVaultName + ".vault.azure.net";
    /**
     * The client to interact with Azure Key Value.
     */
    private static SecretClient secretClient;

    /**
     * Retrieves the value of the secret associated with the specified key from Azure Key Vault.
     *
     * @param keySecretName The key of the secret.
     * @return The value of the secret.
     */

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
