# CI/CD Setup

This repository is wired for the Jenkins + SonarQube + DockerHub + Helm + ArgoCD workflow from the videos, adapted for the existing Maven microservices.

## 1. Start Jenkins and SonarQube

```bash
docker compose up -d --build jenkins sonarqube sonarqube-db
```

- Jenkins: http://localhost:8080
- SonarQube: http://localhost:9001

Get the first Jenkins admin password:

```bash
docker logs jenkins
```

Install these Jenkins plugins:

- Docker Pipeline
- SonarQube Scanner
- Pipeline Stage View
- GitHub

## 2. Jenkins Credentials

Create these global credentials in Jenkins:

- `dockerhub-token`: DockerHub username/password or access token.
- `github-token`: GitHub username/token with repository write access.

The pipeline currently uses `sanchikumar12` as the DockerHub namespace. If your DockerHub username is different, update `DOCKERHUB_NAMESPACE` in `Jenkinsfile` and every image repository in `charts/edulearn/values.yaml`.

## 3. SonarQube

In SonarQube, create a token and configure Jenkins:

- Manage Jenkins -> System -> SonarQube servers
- Name: `sonarqube`
- Server URL from the Jenkins container: `http://sonarqube:9000`
- Add the SonarQube token credential

Create a webhook in SonarQube:

```text
http://jenkins:8080/sonarqube-webhook/
```

Optional quality gate:

- Create a gate named `pass gate`.
- Add conditions that should fail the build, such as code smells greater than `0`.

## 4. Docker Images

Each service now has a Dockerfile. Jenkins builds and pushes these images:

- `discovery-server`
- `auth-service`
- `course-service`
- `lesson-service`
- `enrollment-service`
- `discussion-service`
- `notification-service`
- `payment-service`

Jenkins tags each image with the Git commit hash and `latest`.

## 5. ArgoCD on Minikube

```bash
minikube start --driver=docker
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl port-forward svc/argocd-server -n argocd 8081:443
```

`argocd/application.yaml` points to `https://github.com/sanchikumar12/jepkins-ci-cd.git`. Set `targetRevision` to your deployment branch if it is not `main`.

Apply the app:

```bash
kubectl apply -f argocd/application.yaml
```

## 6. Kubernetes Prerequisites

The Helm chart expects these runtime services to exist in the cluster:

- `mysql` service for MySQL-backed services.
- `kafka` service for Kafka-backed services.

If your DockerHub repository is private, create a pull secret and add it to `charts/edulearn/values.yaml`:

```bash
kubectl create secret docker-registry dockerhub-pull-secret \
  --docker-server=https://index.docker.io/v1/ \
  --docker-username=<username> \
  --docker-password=<token> \
  --namespace edulearn
```

```yaml
global:
  imagePullSecrets:
    - name: dockerhub-pull-secret
```

## 7. Deployment Flow

1. Jenkins checks out GitHub code.
2. Jenkins runs Maven build and tests for every service.
3. Jenkins runs SonarQube analysis and waits for the quality gate.
4. Jenkins builds and pushes Docker images.
5. Jenkins updates `charts/edulearn/values.yaml` with the new image tag and pushes it to GitHub.
6. ArgoCD detects the Helm values change and syncs the Kubernetes deployment.
