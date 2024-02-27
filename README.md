# Azure-Speech-To-Text


Setup key vault:
```powershell
setx KEY_VAULT_NAME "Your-Key-Vault-Name"
```
```powershell
az login
```
```powershell
az keyvault set-policy --name Your-Key-Vault-Name --upn user@domain.com --secret-permissions delete get list set purge
```

