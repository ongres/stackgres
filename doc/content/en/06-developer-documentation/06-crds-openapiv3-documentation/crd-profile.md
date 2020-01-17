# StackGresProfile

The Profile CRD represent the main params for instance resouces.

___

**Kind:** StackGresProfile

**listKind:** StackGresProfileList

**plural:** sgprofiles

**singular:** sgprofile
___

### **Properties**


| Property | Type | Description |
|-----------|------|-------------|
| cpu | string  | CPU amount to be used  |
| memory | string  | Memory size to be used  |


Example:

```
apiVersion: stackgres.io/v1alpha1
  kind: StackGresProfile
  metadata:
    annotations:
      helm.sh/hook: pre-install
    labels:
      app: stackgres
      chart: stackgres-cluster-0.8
      heritage: Tiller
      release: stackgres
    name: size-l
    namespace: default
  spec:
    cpu: "4"
    memory: 8Gi
```