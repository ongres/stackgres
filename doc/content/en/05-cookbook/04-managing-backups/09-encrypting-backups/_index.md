---
title: Encrypting backups at rest
weight: 9
url: /cookbook/managing-backups/encrypting-backups
description: Encrypt backups with OpenPGP or libsodium at the storage level.
showToc: true
---

## What it does

Adds client-side encryption to every file written to an
[SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}). You configure
the encryption method and the key material under `spec.encryption`. The operator
reconciles the change and all subsequent backups and WAL segments are encrypted before
they leave the cluster. Existing objects already in the bucket are not retroactively
encrypted.

## When to use it

- Regulatory or security requirements mandate that backup data is encrypted independently
  of the storage provider's server-side encryption.
- You want end-to-end control of the key material, ensuring that the cloud provider cannot
  read your backups even if it has access to the bucket.
- You already manage PGP or libsodium keys as part of your key-management workflow.

## How to do it

### 1. Store the key in a Kubernetes Secret

**libsodium (recommended for new setups):**

```bash
# Generate a random 32-byte key in hex form
openssl rand -hex 32 | kubectl create secret generic backup-enc-key \
  --namespace my-cluster \
  --from-file=key=/dev/stdin
```

**OpenPGP:**

```bash
# Export the ASCII-armored PGP keypair (public+private) to a file, then:
kubectl create secret generic backup-pgp-key \
  --namespace my-cluster \
  --from-file=key=my-keypair.asc
```

If the private key is passphrase-protected, store the passphrase in the same or a
different Secret:

```bash
kubectl create secret generic backup-pgp-passphrase \
  --namespace my-cluster \
  --from-literal=passphrase='<your-passphrase>'
```

### 2. Patch the SGObjectStorage

Apply the `spec.encryption` section to the existing SGObjectStorage. Below are examples
for each method.

**libsodium:**

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  namespace: my-cluster
  name: my-backup-storage
spec:
  # ... existing type and bucket configuration unchanged ...
  encryption:
    method: sodium              # use libsodium symmetric encryption
    sodium:
      key:
        name: backup-enc-key   # Secret containing the 32-byte key
        key: key               # key within that Secret
      keyTransform: hex        # tells the operator the value is hex-encoded
```

**OpenPGP (no passphrase):**

```yaml
spec:
  encryption:
    method: openpgp
    openpgp:
      key:
        name: backup-pgp-key   # Secret holding the ASCII-armored keypair
        key: key
```

**OpenPGP (passphrase-protected private key):**

```yaml
spec:
  encryption:
    method: openpgp
    openpgp:
      key:
        name: backup-pgp-key
        key: key
      keyPassphrase:
        name: backup-pgp-passphrase   # Secret holding the passphrase
        key: passphrase
```

Apply with:

```bash
kubectl apply -f my-backup-storage.yaml
```

## How it works

The `spec.encryption` section is part of
[SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}), which is
reconciled by the StackGres operator. After the update is accepted, pgbackrest and the WAL
shipping process are reconfigured to encrypt each file before uploading. For `sodium`, a
single 32-byte symmetric key is used (XSalsa20-Poly1305). For `openpgp`, the keypair is
used to encrypt each file to the public key; the private key (and optional passphrase) is
needed for restore. The `keyTransform` field (`none` / `hex` / `base64`, default `none`)
controls how the raw bytes stored in the Secret are decoded to produce the 32-byte sodium
key.

## What to expect

- After applying the change, the next scheduled or on-demand backup will be encrypted.
  The SGObjectStorage resource moves to a reconciled state immediately; no cluster restart
  is needed.
- Restore operations (including PITR) automatically decrypt using the key material
  referenced in the SGObjectStorage, so recovery works transparently as long as the Secret
  is available.
- Encrypted backups are slightly larger due to authentication overhead and any encoding
  applied to the key.

## Pitfalls

- **Key loss is permanent data loss.** The operator stores only a reference to the Secret,
  not the key itself. If the Secret is deleted and you have no out-of-band copy of the key
  material, all backups encrypted with it become unrecoverable. Back up key material to a
  secure, separate store (e.g., a secrets manager or HSM).
- **Existing objects are not re-encrypted.** Only objects written after the configuration
  change are encrypted. Backups taken before the change remain in their original form.
  Similarly, removing `spec.encryption` does not decrypt existing backups.
- **Mixing encrypted and unencrypted backups.** If you switch methods or remove
  encryption, older backups in the bucket still require the original key to restore.
  Retain the old Secret until all backups that used it have expired or been deleted.
- **`keyTransform: none` is a legacy mode.** It pads or truncates the raw secret bytes to
  32 bytes, which may reduce effective key entropy. Prefer `hex` or `base64` for new
  deployments.
- **Passphrase-less PGP keys in production.** Storing an unprotected private key in a
  Kubernetes Secret means anyone who can read that Secret can decrypt your backups. Use
  RBAC to restrict access or protect the private key with a passphrase.
