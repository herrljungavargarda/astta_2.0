# Azure-Speech-To-Text-Analysis

This project is made by three students developers from YRGO on behalf on Herrljunga and Vårgårda municipality,
for use to transcribe and analyze calls coming to service desk.

## Before use there are some requirements:

- Azure Speech To Text
- Azure OpenAi
- Blob storage (or any place to store audio/text files)
- Key vault to store secrets and keys (can be stored in any way you wish)

------------

## Configuring

- Set up necessary keys and refer to them in Utils/Config (if using key vault, refer to the name you set)
- Set up Ai model, can be found and set up when signed in to Azure Portal under Azure OpenAi, Model deployments

### Key vault setup

| Key                  | Description                                                                                    |
|----------------------|------------------------------------------------------------------------------------------------|
| `Blob SAS token`     | Can be generated when signed in to Azure Portal under Storage account, Shared access signature |
| `Open AI key`        | Can be found when signed in to Azure Portal under Azure OpenAi, Keys and Endpoint              |
| `Speech to text key` | Can be found when signed in to Azure Portal under Speech service, Keys and Endpoint            |
------------
![keyvault](./readmeResources/keyvault.png)

### Setup key vault authentication on local machine:

To run az commands in Powershell or other terminal you need to install [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli)

```Shell
setx KEY_VAULT_NAME "Your-Key-Vault-Name"
```

```Shell
az login
```

```Shell
az keyvault set-policy --name Your-Key-Vault-Name --upn user@domain.com --secret-permissions delete get list set purge
```

------------

## How the application works:

#### 1. Fetching an audio file from dedicated storage

#### 2. Transcribes that file using Azure Speech To Text

#### 3. Analyzing the transcribed text using Azure OpenAi

#### 4. Saving output to dedicated storage as json format
