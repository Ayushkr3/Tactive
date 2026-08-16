# Scheduler User Guide

## Start the application

Install Docker Desktop, then from the repository root run:

```bash
docker compose up --build
```

Open [http://localhost:8080](http://localhost:8080). Register an account, then sign in.

## Schedule work

1. Create a **project** to group related work.
2. Create a **queue** inside the project. Set its priority, retry policy, and whether it is paused.
3. Open the queue and create a job. Choose a registered job type, add its payload, and either run it immediately or select a future time.
4. Watch the **Jobs** page for its status: `SCHEDULED`, `QUEUED`, `CLAIMED`, `RUNNING`, and finally `COMPLETED`.

## Recurring work

On the **Recurring** page, create a recurring definition with a six-field Spring cron expression. The scheduler creates a normal job each time that expression becomes due. Disable the definition to pause future runs without deleting its history.

## When a job fails

The scheduler records every attempt and retries using the queue or job retry policy. A job that reaches its retry limit is moved to the **Dead Letter Queue**. From there, inspect the failure reason and requeue it after fixing the underlying problem.

## Monitoring workers

The **Workers** page shows registered workers, their active job counts, and latest heartbeat. A worker that stops heartbeating is marked stopped, and orphaned jobs are reclaimed automatically.

## Important notes

- Use the built-in registration and sign-in flow; authenticated API calls require the JWT returned at login.
- Do not put passwords, tokens, or other secrets in job payloads or commits.
- The application validates request data, but a job handler should still treat payload input as untrusted.
