# 📑 TECHNICAL NOTES: CI/CD PIPELINE FOR TASK MANAGER

* **Trigger:** Runs automatically on every code push to the `main` branch.
* **Architecture:** 3 Sequential/Dependent Jobs running on GitHub-hosted Runners (`ubuntu-latest`).

---

## 🏗️ PIPELINE VISUALIZATION

```
[ Push to main ]
       │
       ▼
 1. BUILD & TEST (H2 DB)
       │
       ▼ (if success)
 2. BUNDLE & PUSH (Docker Hub via Buildx with Cache)
       │
       ▼ (if success)
 3. DEPLOY TO HOMELAB (Via Tailscale VPN Mesh Network)
       │
       ├──► [SSH Connect] ──► [Pull Image] ──► [Graceful Stop: 30s]
       │                                            │
       └──► [Verify Deployment] ◄── [Actuator Health Check Loop] ◄┘
```

---

## 🛠️ JOB 1: Build & Test (`build-and-test`)
**Purpose:** Ensures code quality, correctness, and compilation validity before any deployment actions occur.

* **Checkout Code:** Pulls the repository code into the runner via `actions/checkout@v4`.
* **Set Up Java 21:** Configures the environment with **Eclipse Temurin JDK 21**. It enables Maven caching (`cache: 'maven'`) to speed up future workflow runs.
* **Test Execution:** Runs `./mvnw clean verify` inside the `./backend` directory.
* **Environment Profile:** Uses `-Dspring.profiles.active=dev` to trigger testing configurations (utilizing an in-memory **H2 Database** for integration tests).

---

## 📦 JOB 2: Package & Push (`build-and-push`)
**Purpose:** Containerizes the application and securely stores it in a central repository.
**Dependency:** `needs: build-and-test` *(Will not execute if tests fail).*

* **Git SHA Generation:** Extracts a short 7-character commit hash (`git rev-parse --short HEAD`) to use as an explicit, immutable version tag.
* **Docker Buildx Setup:** Initializes Docker Buildx to enable multi-platform builds and layer caching.
* **Secure Authentication:** Logs into Docker Hub utilizing encrypted GitHub Secrets (`DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`).
* **Metadata Extraction:** Automatically generates two distinct image tags:
    1. `sha-<commit-hash>` (For exact version control and rollback safety).
    2. `latest` (Points to the most recent successful build).
* **Optimized Docker Build:** Builds the image using the `./backend` context and pushes it to Docker Hub. It utilizes **Registry Cache** (`cache-from`/`cache-to`) to minimize build times.

---

## 🚀 JOB 3: Deploy to Homelab (`deploy-to-homelab`)
**Purpose:** Delivers the updated application into the secure private homelab environment.
**Dependency:** `needs: [build-and-test, build-and-push]`

### 🔒 Section A: Secure Networking (Tailscale)
* Connects the isolated GitHub Actions runner directly into the private homelab mesh network using **Tailscale OAuth credentials**.
* This removes the security risk of exposing SSH ports (`22`) openly to the public internet.

### ⚙️ Section B: Remote Execution & Environment Setup
* Dynamically generates a temporary local SSH configuration (`~/.ssh/config`) to safely authenticate against the target server using private keys.
* Securely injects runtime environment variables over SSH:
    * `APP_PORT`: Explicitly set to `8081`.
    * `APP_VERSION`: Bound strictly to the specific `sha-<commit-hash>`.

### 🔄 Section C: Deployment Lifecycle
```
[Pull Image] ➔ [Graceful Stop (30s)] ➔ [Container Start] ➔ [Health Check Loop] ➔ [Prune]
```
1. **Pull:** Fetches the exact target image tag from Docker Hub onto the homelab server.
2. **Graceful Stop:** Grants the existing backend container up to 30 seconds (`-t 30`) to finish processing active API transactions before shutting down.
3. **Start:** Boots the updated backend container detached (`-d`) via Docker Compose.
4. **Health Check Loop:** Executes an automated `while` loop poking the Actuator health endpoint (`/api/v1/actuator/health`). It polls up to **30 times** (1-second intervals). If it receives an HTTP 200, it marks the deployment as a success; if it hits the timeout, it dumps container logs and aborts with an error.
5. **Cleanup:** Runs `docker image prune -f` to wipe out dangling or unused intermediate Docker layers, preserving disk space on the homelab hardware.

---

## 🔍 POST-VERIFICATION: Verify Deployment
* Executes an independent connection check right after the deployment script finishes.
* Prints out a status table of all active containers containing the pattern `"name=task"`.
* Directly queries the Actuator readiness endpoint (`/actuator/health/ready`) to confirm that the application is fully operational and capable of accepting incoming database queries or API router traffic.

---

### 💡 CORE TAKEAWAYS FOR NOTEBOOK
* **Security Isolation:** Architecture relies entirely on **Tailscale** for ingress, meaning the server firewall remains secure.
* **Zero Hardcoded Secrets:** Production DB secrets stay resident on the homelab host server; pipeline configurations rely entirely on transient shell exports.
* **Self-Healing Guardrails:** The integration of **Actuator health checks** directly in the bash routine guarantees that if a bad build crashes on startup, the pipeline instantly errors out rather than silently failing.