---
title: Network Security
weight: 4
url: /administration/security/network
description: Network policies and service exposure security for StackGres clusters.
showToc: true
---

This guide covers network security configuration for StackGres clusters, including Network Policies, service exposure, and secure access patterns.

## Default Network Configuration

By default, StackGres creates the following services for each cluster:

| Service | Type | Purpose |
|---------|------|---------|
| `<cluster>` | ClusterIP | Read-write (primary) connections |
| `<cluster>-primary` | ClusterIP | Explicit primary connections |
| `<cluster>-replicas` | ClusterIP | Read-only (replica) connections |

These services are only accessible within the Kubernetes cluster.

## Network Policies

Network Policies restrict pod-to-pod communication, implementing a zero-trust network model.

### Deny All by Default

Start with a deny-all policy:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-all
  namespace: production
spec:
  podSelector: {}
  policyTypes:
    - Ingress
    - Egress
```

### Allow StackGres Cluster Communication

Allow communication between cluster pods:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-stackgres-cluster
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: StackGresCluster
      stackgres.io/cluster-name: my-cluster
  policyTypes:
    - Ingress
    - Egress
  ingress:
    # Allow from other cluster pods (replication)
    - from:
        - podSelector:
            matchLabels:
              app: StackGresCluster
              stackgres.io/cluster-name: my-cluster
      ports:
        - protocol: TCP
          port: 5432
        - protocol: TCP
          port: 8008  # Patroni REST API
    # Allow from operator
    - from:
        - namespaceSelector:
            matchLabels:
              name: stackgres
          podSelector:
            matchLabels:
              app: stackgres-operator
  egress:
    # Allow to other cluster pods
    - to:
        - podSelector:
            matchLabels:
              app: StackGresCluster
              stackgres.io/cluster-name: my-cluster
      ports:
        - protocol: TCP
          port: 5432
        - protocol: TCP
          port: 8008
    # Allow DNS
    - to:
        - namespaceSelector: {}
          podSelector:
            matchLabels:
              k8s-app: kube-dns
      ports:
        - protocol: UDP
          port: 53
```

### Allow Application Access

Allow specific applications to connect:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-app-to-postgres
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: StackGresCluster
      stackgres.io/cluster-name: my-cluster
  policyTypes:
    - Ingress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: myapp
      ports:
        - protocol: TCP
          port: 5432
```

### Allow Backup Access

Allow backup pods to access object storage:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-backup-egress
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: StackGresBackup
  policyTypes:
    - Egress
  egress:
    # Allow HTTPS to object storage
    - to:
        - ipBlock:
            cidr: 0.0.0.0/0
      ports:
        - protocol: TCP
          port: 443
    # Allow DNS
    - to:
        - namespaceSelector: {}
          podSelector:
            matchLabels:
              k8s-app: kube-dns
      ports:
        - protocol: UDP
          port: 53
```

## Exposing Services

### Internal Load Balancer

Expose within your private network:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-cluster-internal-lb
  annotations:
    # AWS
    service.beta.kubernetes.io/aws-load-balancer-internal: "true"
    # GCP
    cloud.google.com/load-balancer-type: "Internal"
    # Azure
    service.beta.kubernetes.io/azure-load-balancer-internal: "true"
spec:
  type: LoadBalancer
  selector:
    app: StackGresCluster
    stackgres.io/cluster-name: my-cluster
    role: master
  ports:
    - port: 5432
      targetPort: 5432
```

### External Access via Ingress

For web console or pgAdmin access, use an Ingress with authentication:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: stackgres-ui
  annotations:
    nginx.ingress.kubernetes.io/auth-type: basic
    nginx.ingress.kubernetes.io/auth-secret: stackgres-basic-auth
spec:
  rules:
    - host: stackgres.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: stackgres-restapi
                port:
                  number: 443
  tls:
    - hosts:
        - stackgres.example.com
      secretName: stackgres-tls
```

### Port Forwarding (Development)

For development/debugging, use port forwarding:

```bash
# Access primary
kubectl port-forward svc/my-cluster 5432:5432

# Access replicas
kubectl port-forward svc/my-cluster-replicas 5433:5432
```

## Pod Security Standards

### Restricted Pod Security

Apply restricted Pod Security Standards:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: production
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/warn: restricted
```

Note: StackGres pods may require certain capabilities. If using restricted mode, you may need to create exceptions:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: production
  labels:
    pod-security.kubernetes.io/enforce: baseline
    pod-security.kubernetes.io/warn: restricted
```

## Service Mesh Integration

### Istio

For Istio service mesh integration, see the [Istio guide]({{% relref "04-administration-guide/02-cluster-creation/05-service-mesh-integration/01-istio" %}}).

Key configurations:
- mTLS for all traffic
- Authorization policies for database access
- Traffic policies for connection management

### Linkerd

For Linkerd integration:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
  annotations:
    linkerd.io/inject: enabled
spec:
  # ...
```

## Firewall Rules

### Cloud Provider Firewalls

If exposing services externally, configure cloud firewalls:

**AWS Security Groups:**
```bash
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxx \
  --protocol tcp \
  --port 5432 \
  --source-group sg-yyyyy
```

**GCP Firewall Rules:**
```bash
gcloud compute firewall-rules create allow-postgres \
  --allow tcp:5432 \
  --source-ranges 10.0.0.0/8 \
  --target-tags postgres
```

**Azure Network Security Groups:**
```bash
az network nsg rule create \
  --resource-group myRG \
  --nsg-name myNSG \
  --name allow-postgres \
  --priority 100 \
  --destination-port-ranges 5432 \
  --source-address-prefixes 10.0.0.0/8
```

## Secure Connection Patterns

### Connection via Bastion

For secure external access:

```
Client -> Bastion Host -> Kubernetes Service -> PostgreSQL
```

```bash
# SSH tunnel through bastion
ssh -L 5432:my-cluster.production.svc.cluster.local:5432 bastion@bastion.example.com

# Connect locally
psql -h localhost -p 5432 -U postgres
```

### VPN Access

Configure VPN to access Kubernetes services:

1. Set up VPN (WireGuard, OpenVPN, etc.)
2. Configure routing to cluster service CIDR
3. Access services directly using cluster DNS

### Private Endpoints

For cloud-managed Kubernetes:

- **AWS EKS**: Use VPC endpoints for private API access
- **GKE**: Use Private Google Access
- **AKS**: Use Private Link

## Monitoring Network Security

### Track Connection Attempts

Monitor PostgreSQL connections:

```sql
-- View current connections
SELECT * FROM pg_stat_activity;

-- Enable connection logging
ALTER SYSTEM SET log_connections = 'on';
ALTER SYSTEM SET log_disconnections = 'on';
SELECT pg_reload_conf();
```

### Network Policy Logs

Enable network policy logging (CNI-dependent):

```yaml
# Calico example
apiVersion: projectcalico.org/v3
kind: GlobalNetworkPolicy
metadata:
  name: log-denied
spec:
  types:
    - Ingress
  ingress:
    - action: Log
    - action: Deny
```

## Best Practices

1. **Default Deny**: Start with deny-all network policies

2. **Principle of Least Privilege**: Only allow necessary connections

3. **Use Internal Load Balancers**: Never expose databases directly to the internet

4. **Enable TLS**: Always use SSL/TLS for connections

5. **Segment Networks**: Use separate namespaces/networks for different environments

6. **Monitor and Audit**: Log and monitor all connection attempts

7. **Regular Reviews**: Periodically review network policies and access
