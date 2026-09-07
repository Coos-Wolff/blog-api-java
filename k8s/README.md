# Kubernetes manifests

These manifests are namespace-portable: none of them hardcode a `metadata.namespace`.
Create the namespace and pass `-n <namespace>` on every command yourself.

## Prerequisites

The Deployment uses `image: blog-api-java:0.0.1` with `imagePullPolicy: IfNotPresent`,
so the image must already be present in the cluster's node (e.g. built locally into
minikube's Docker daemon, or loaded with `minikube image load`).

Copy the Secret template and fill in real values — `secret.example.yaml` contains
only `CHANGEME` placeholders and is not usable as-is:

```sh
cp secret.example.yaml secret.yaml   # do not commit secret.yaml
```

## Apply order

```sh
kubectl create namespace blog-api-java
NS=blog-api-java

kubectl apply -n $NS -f secret.yaml                        # 1. credentials
kubectl apply -n $NS -f headless-clusterip-service.yaml    # 2. stable postgres DNS
kubectl apply -n $NS -f postgres-stateful-set.yaml          # 3. database
kubectl apply -n $NS -f config-map.yaml                     # 4. app config
kubectl apply -n $NS -f deployment.yaml                     # 5. app
kubectl apply -n $NS -f svc.yaml                            # 6. app service
```

The Secret comes first because both the StatefulSet and the Deployment consume it via
`envFrom`. The headless Service comes before the StatefulSet so the per-pod DNS name
`postgres-0.postgres.<namespace>.svc.cluster.local` resolves as soon as the pod starts.

## Namespace coupling in DB_HOST

`config-map.yaml` sets:

```yaml
DB_HOST: postgres-0.postgres.blog-api-java.svc.cluster.local
```

That FQDN embeds the namespace (`blog-api-java`). Deploying into a different namespace
means editing this value to match — stripping `metadata.namespace` does not make this
string portable. Within a single namespace the short name `postgres-0.postgres` also
works and avoids the coupling.

## Access

`svc.yaml` is a `ClusterIP`, so reach the API from outside the cluster with a port-forward:

```sh
kubectl port-forward -n $NS svc/blog-api-java 8080:8080
```
