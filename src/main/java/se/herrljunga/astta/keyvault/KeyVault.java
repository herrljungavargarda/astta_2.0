package se.herrljunga.astta.keyvault;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;

/**
 * The KeyVault class.
 *
 * This class provides methods to interact with Azure Key Vault to retrieve secrets.
 * It uses the Azure Key Vault SDK for Java to interact with Azure Key Vault.
 * The class is initialized with the name of the Key Vault, which is retrieved from an environment variable.
 * It provides a method to get the value of a secret from the Key Vault.
 */
public class KeyVault {
    /**
     * The name of the Key Vault, retrieved from an environment variable.
     */
    private static final String keyVaultName = System.getenv("KEY_VAULT_NAME");

    /**
     * The URI of the Key Vault, constructed from the Key Vault name.
     */
    private static final String keyVaultUri = "https://" + keyVaultName + ".vault.azure.net";

    /**
     * The client used to interact with Azure Key Vault.
     * This client is an instance of SecretClient from the Azure Key Vault SDK for Java.
     */
    private static SecretClient secretClient;

    /**
     * Retrieves the value of the secret associated with the specified key from Azure Key Vault.
     *
     * This method uses the SecretClient to get the secret from the Key Vault.
     * If the SecretClient has not been initialized, it initializes it with the Key Vault URI and a DefaultAzureCredential.
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