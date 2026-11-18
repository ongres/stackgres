---
title: Backup Encryption
weight: 5
url: /administration/backups/encryption
description: How to encrypt backups at rest using libsodium or OpenPGP.
showToc: true
---

StackGres supports encrypting backups at rest before they are stored in object storage. This provides an additional layer of security for your backup data, ensuring that even if your storage is compromised, the backup contents remain protected.

## Encryption Methods

StackGres supports two encryption methods:

| Method | Description | Key Type | Use Case |
|--------|-------------|----------|----------|
| **sodium** | Uses libsodium symmetric encryption | 32-byte secret key | Simple setup, high performance |
| **openpgp** | Uses OpenPGP standard encryption | PGP key pair | Industry standard, key management flexibility |

## Sodium Encryption

Sodium encryption uses the [libsodium](https://doc.libsodium.org/) library for symmetric encryption. It requires a single 32-byte secret key for both encryption and decryption.

### Generating a Key

Generate a secure random key using one of these methods:

```bash
# Generate a hex-encoded key (recommended)
openssl rand -hex 32

# Or generate a base64-encoded key
openssl rand -base64 32
```

### Creating the Secret

Store the encryption key in a Kubernetes Secret:

```bash
# Using hex-encoded key
kubectl create secret generic backup-encryption-key \
  --from-literal=key=$(openssl rand -hex 32)
```

Or using a YAML manifest:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: backup-encryption-key
type: Opaque
stringData:
  key: "your-64-character-hex-encoded-key-here"
```

### Configuring SGObjectStorage with Sodium

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: encrypted-storage
spec:
  type: s3
  encryption:
    method: sodium
    sodium:
      key:
        name: backup-encryption-key
        key: key
      keyTransform: hex  # or 'base64' or 'none'
  s3:
    bucket: my-encrypted-backups
    awsCredentials:
      secretKeySelectors:
        accessKeyId:
          name: s3-backup-secret
          key: accessKeyId
        secretAccessKey:
          name: s3-backup-secret
          key: secretAccessKey
```

### Key Transform Options

The `keyTransform` field specifies how the key value should be interpreted:

| Value | Description |
|-------|-------------|
| `hex` | Key is hex-encoded (64 hex characters = 32 bytes) |
| `base64` | Key is base64-encoded |
| `none` | Key is used as-is (truncated or zero-padded to 32 bytes) |

**Recommendation**: Use `hex` or `base64` for new setups. The `none` option exists for backwards compatibility.

## OpenPGP Encryption

OpenPGP encryption uses the industry-standard PGP protocol, allowing you to use existing PGP key management practices.

### Generating a PGP Key Pair

Generate a new PGP key pair:

```bash
# Generate a new key pair (follow the prompts)
gpg --full-generate-key

# Export the private key (armored format)
gpg --armor --export-secret-keys your@email.com > private-key.asc

# Export the public key (for reference)
gpg --armor --export your@email.com > public-key.asc
```

For automated environments, generate without interaction:

```bash
cat > key-params <<EOF
%no-protection
Key-Type: RSA
Key-Length: 4096
Subkey-Type: RSA
Subkey-Length: 4096
Name-Real: StackGres Backup
Name-Email: backup@stackgres.local
Expire-Date: 0
%commit
EOF

gpg --batch --generate-key key-params
gpg --armor --export-secret-keys backup@stackgres.local > private-key.asc
```

### Creating the Secret

Store the PGP private key in a Kubernetes Secret:

```bash
kubectl create secret generic backup-pgp-key \
  --from-file=private-key=private-key.asc
```

If your key has a passphrase:

```bash
kubectl create secret generic backup-pgp-key \
  --from-file=private-key=private-key.asc \
  --from-literal=passphrase='your-key-passphrase'
```

### Configuring SGObjectStorage with OpenPGP

Without passphrase:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: encrypted-storage
spec:
  type: s3
  encryption:
    method: openpgp
    openpgp:
      key:
        name: backup-pgp-key
        key: private-key
  s3:
    bucket: my-encrypted-backups
    awsCredentials:
      secretKeySelectors:
        accessKeyId:
          name: s3-backup-secret
          key: accessKeyId
        secretAccessKey:
          name: s3-backup-secret
          key: secretAccessKey
```

With passphrase:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: encrypted-storage
spec:
  type: s3
  encryption:
    method: openpgp
    openpgp:
      key:
        name: backup-pgp-key
        key: private-key
      keyPassphrase:
        name: backup-pgp-key
        key: passphrase
  s3:
    bucket: my-encrypted-backups
    awsCredentials:
      secretKeySelectors:
        accessKeyId:
          name: s3-backup-secret
          key: accessKeyId
        secretAccessKey:
          name: s3-backup-secret
          key: secretAccessKey
```

## Complete Example: Encrypted S3 Backups

Here's a complete example setting up encrypted backups to AWS S3:

### 1. Create the Encryption Key

```bash
# Generate and store sodium key
kubectl create secret generic backup-encryption-key \
  --from-literal=key=$(openssl rand -hex 32)
```

### 2. Create S3 Credentials

```bash
kubectl create secret generic s3-backup-secret \
  --from-literal=accessKeyId=AKIAIOSFODNN7EXAMPLE \
  --from-literal=secretAccessKey=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

### 3. Create SGObjectStorage with Encryption

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: encrypted-s3-storage
spec:
  type: s3
  encryption:
    method: sodium
    sodium:
      key:
        name: backup-encryption-key
        key: key
      keyTransform: hex
  s3:
    bucket: my-encrypted-backups
    region: us-west-2
    awsCredentials:
      secretKeySelectors:
        accessKeyId:
          name: s3-backup-secret
          key: accessKeyId
        secretAccessKey:
          name: s3-backup-secret
          key: secretAccessKey
```

### 4. Configure Cluster Backups

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  instances: 3
  postgres:
    version: '16'
  pods:
    persistentVolume:
      size: '10Gi'
  configurations:
    backups:
      - sgObjectStorage: encrypted-s3-storage
        cronSchedule: '0 5 * * *'
        retention: 7
```

## Encryption with Other Storage Backends

Encryption works with all supported storage backends. Here are examples for each:

### Azure Blob Storage with Encryption

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: encrypted-azure-storage
spec:
  type: azureBlob
  encryption:
    method: sodium
    sodium:
      key:
        name: backup-encryption-key
        key: key
      keyTransform: hex
  azureBlob:
    bucket: my-container
    azureCredentials:
      secretKeySelectors:
        storageAccount:
          name: azure-backup-secret
          key: storageAccount
        accessKey:
          name: azure-backup-secret
          key: accessKey
```

### Google Cloud Storage with Encryption

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: encrypted-gcs-storage
spec:
  type: gcs
  encryption:
    method: openpgp
    openpgp:
      key:
        name: backup-pgp-key
        key: private-key
  gcs:
    bucket: my-encrypted-bucket
    gcpCredentials:
      secretKeySelectors:
        serviceAccountJSON:
          name: gcs-backup-secret
          key: service-account.json
```

## Key Management Best Practices

1. **Secure Key Storage**: Store encryption keys in a secure secrets management system (e.g., HashiCorp Vault, AWS Secrets Manager) and sync to Kubernetes Secrets.

2. **Key Rotation**: Periodically rotate encryption keys. When rotating:
   - Create a new SGObjectStorage with the new key
   - Take a new backup with the new configuration
   - Keep the old key available for restoring old backups

3. **Key Backup**: Always maintain a secure backup of your encryption keys outside of Kubernetes. Without the key, encrypted backups cannot be restored.

4. **Access Control**: Use Kubernetes RBAC to restrict access to encryption key Secrets.

5. **Audit Logging**: Enable audit logging for Secret access to track who accesses encryption keys.

## Restoring Encrypted Backups

Encrypted backups are automatically decrypted during restore operations, provided the same SGObjectStorage configuration (with encryption settings) is used.

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: restored-cluster
spec:
  instances: 3
  postgres:
    version: '16'
  pods:
    persistentVolume:
      size: '10Gi'
  initialData:
    restore:
      fromBackup:
        name: encrypted-backup-name
```

The restore process will:
1. Read the backup from object storage
2. Decrypt using the key from the SGObjectStorage configuration
3. Restore to the new cluster
