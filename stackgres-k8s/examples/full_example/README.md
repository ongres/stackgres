<div align="center">
   <h1>StackGres</h1>
   <p><b>Enterprise Postgres made easy. On Kubernetes</b></p>
   <a href="https://stackgres.io" target="_blank">
      <img src="https://stackgres.io/img/favicon/android-chrome-192x192.png" alt="StackGres"/>
   </a>
</div>


# Environment

The environment specify for this example is a k8s cluster using GKE with 3 instances dedicated for the SGCluster using a instance slug `g-40vcpu-160gb`

>Note: You must adjust the configuration according to your instances sizes. 

# Requirements

- [Helm](https://helm.sh/docs/intro/install/) => v3.7.2
- [Helm-diff plugin](https://github.com/databus23/helm-diff)
- [helmfile](https://github.com/roboll/helmfile) => v0.143.0
- [kubectl](https://kubernetes.io/docs/tasks/tools/)


# Create all resources

## 1- Install Operator

```
helmfile -f operator/helmfile.yaml apply
```

## 2- Create all the configurations

```
kubectl apply -f configurations/.
```

## 3- Create SGCluster

```
kubectl apply -f cluster/SGCluster.yaml
```