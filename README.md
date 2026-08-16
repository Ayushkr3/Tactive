# Scheduler

Scheduler is a Spring Boot and PostgreSQL distributed job scheduler. It supports projects, queues, immediate and delayed jobs, batches, recurring jobs, retries, worker heartbeats, execution history, logs, and a dead-letter queue (DLQ).

## Features

* JWT-based registration and login
* Multi-tenant projects and queues
* Immediate, delayed, scheduled, batch, and recurring jobs
* Priority-based PostgreSQL job claiming with `FOR UPDATE SKIP LOCKED`
* Configurable retry strategies: `FIXED`, `LINEAR`, and `EXPONENTIAL`
* Separate API and worker application modes
* Worker monitoring and stale-job recovery
* Dead-letter queue with manual requeue
* Browser dashboard served by the application

## Requirements

* Docker and Docker Compose (recommended)
* Or Java 25, Maven, and PostgreSQL 16 for local development

## Testing

Tests are run locally and in GitHub Actions using Maven:

```bash
./mvnw.cmd test
```

## Run with Docker Compose

The build-based Compose file builds the application image locally:

```bash
git clone https://github.com/Ayushkr3/Tactive.git
cd Tactive
docker compose up
```

This starts:

* PostgreSQL on `localhost:5432`
* The API and dashboard on `http://localhost:8080`
* A separate worker connected to the same database

Open `http://localhost:8080`, register an account, and sign in. Stop the stack with:

```bash
docker compose down
```

To remove the persisted database volume as well, use `docker compose ... down -v`.

The root `docker-compose.yml` uses the published image `ghcr.io/ayushkr3/scheduler:latest` instead of building locally:

```bash
docker compose up -d
```

## Basic workflow

1. Register or log in.
2. Create a project.
3. Create a queue in the project.
4. Add a job to the queue.
5. Monitor the job from the dashboard or API.

Jobs progress through states such as `SCHEDULED`, `QUEUED`, `CLAIMED`, `RUNNING`, `COMPLETED`, `FAILED`, `DEAD\_LETTER`, or `CANCELED`.

## API quick start

Register and save the returned token:

```bash
curl -X POST http://localhost:8080/api/auth/register \\
  -H 'Content-Type: application/json' \\
  -d '{"name":"Example User","email":"user@example.com","password":"password123"}'
```

Log in when the account already exists:

```bash
curl -X POST http://localhost:8080/api/auth/login \\
  -H 'Content-Type: application/json' \\
  -d '{"email":"user@example.com","password":"password123"}'
```

Authenticated endpoints use the token as follows:

```bash
curl http://localhost:8080/api/projects \\
  -H "Authorization: Bearer $TOKEN"
```

The main endpoint groups are:

|Group|Endpoints|
|-|-|
|Authentication|`/api/auth/register`, `/api/auth/login`|
|Projects|`/api/projects`|
|Queues|`/api/projects/{projectId}/queues`, `/api/queues/{queueId}`|
|Jobs|`/api/queues/{queueId}/jobs`|
|Recurring jobs|`/api/queues/{queueId}/recurring-jobs`|
|DLQ|`/api/dlq`, `/api/dlq/{id}/requeue`|
|Workers|`/api/workers`|

For request and response details, see [`docs/PROJECT\_DOCUMENTATION.md`](docs/PROJECT_DOCUMENTATION.md).

## Job handlers

The job `type` must match the name of a Spring `JobHandler` bean registered in the worker. The included handler is `programExecute`:

```bash
curl -X POST http://localhost:8080/api/queues/$QUEUE\_ID/jobs \\
  -H "Authorization: Bearer $TOKEN" \\
  -H 'Content-Type: application/json' \\
  -d '{"type":"programExecute","payLoad":"{\\"path\\":\\"/usr/bin/true\\"}"}'
```

`programExecute` starts a process on the worker. Only use it with trusted input; arbitrary payloads can execute arbitrary programs. Add custom handlers by implementing `JobHandler` and registering the implementation as a Spring component.

## Recurring jobs

Recurring jobs use six-field Spring cron expressions, including seconds. For example, `0 0 6 \* \* \*` runs daily at 06:00:

```json
{
  "name": "daily-report",
  "type": "programExecute",
  "payload": "{\\"path\\":\\"/usr/bin/true\\"}",
  "cronExpression": "0 0 6 \* \* \*"
}
```

## Documentation

* [`docs/USER_GUIDE.md`](docs/USER_GUIDE.md) — dashboard-focused usage guide
* [`docs/PROJECT_DOCUMENTATION.md`](docs/PROJECT_DOCUMENTATION.md) — architecture, lifecycle, schema, API, and handler details
* [`docs/Distributed_Job_Scheduler_Presentation.pptx`](docs/Distributed_Job_Scheduler_Presentation.pptx) — Deck presentation
* [`docs/AI_CHANGE_LOOP_EVIDENCE.md`](docs/AI_CHANGE_LOOP_EVIDENCE.md) — AI change loop evidence
## Configuration and security notes

* The default sample credentials and database password are for development only.
* The JWT signing key is currently defined in code; externalize and rotate it before deployment. Do not commit secrets or place them in job payloads.
* Job handlers should validate payloads and be idempotent because a worker crash can cause a job to run again.
* Review and restrict the current permissive HTTP security configuration before exposing the service publicly.

