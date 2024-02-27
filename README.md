# Azure-Speech-To-Text


Setup key vault:
```Shell
setx KEY_VAULT_NAME "Your-Key-Vault-Name"
```
```Shell
az login
```
```Shell
az keyvault set-policy --name Your-Key-Vault-Name --upn user@domain.com --secret-permissions delete get list set purge
```

