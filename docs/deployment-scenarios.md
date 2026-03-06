# Deployment Scenarios

This document explains how to run the shared `core-backend` codebase in the three target scenarios:

1. Cloud only
2. Edge only (offline)
3. Edge + Cloud (hybrid sync)

It also maps each scenario to the current runtime-role implementation.

## Runtime model

The backend has one runtime role per process:

- `edge`
- `cloud`

This is enforced by profile validation in:

- `common/configs/RuntimeRoleGuard`
- `common/configs/RuntimeDeploymentContext`

You must not run both profiles in one process.

## Configuration keys

Primary controls:

- `SPRING_PROFILES_ACTIVE` (`edge` or `cloud`)
- `app.runtime.scenario` (`AUTO`, `CLOUD_ONLY`, `EDGE_ONLY`, `HYBRID`)
- `app.sync.edge.cloud-base-url`
- `app.sync.edge.worker-enabled`
- `app.sync.edge.capture-enabled`

Database controls:

- `ZIVAI_DB_URL`
- `ZIVAI_DB_USERNAME`
- `ZIVAI_DB_PASSWORD`

Sync identity/auth (edge side):

- `ZIVAI_EDGE_NODE_ID`
- `ZIVAI_EDGE_AUTH_KEY`
- `ZIVAI_CLOUD_SYNC_BASE_URL`

## Scenario 1: Cloud only (good internet)

Use when the school has reliable internet and can use cloud services directly.

Behavior:

- Run backend with `cloud` role.
- No edge outbox capture.
- No edge worker polling cloud.
- Cloud exposes sync APIs for edge nodes that may connect later.

Recommended settings:

- `SPRING_PROFILES_ACTIVE=cloud`
- `app.runtime.scenario=CLOUD_ONLY` (or `AUTO`)
- `app.sync.edge.worker-enabled=false`
- `app.sync.edge.capture-enabled=false`

Example:

```bash
cd core-backend
export SPRING_PROFILES_ACTIVE=cloud
export ZIVAI_DB_URL='jdbc:postgresql://<cloud-host>:5432/<cloud-db>'
export ZIVAI_DB_USERNAME='<cloud-user>'
export ZIVAI_DB_PASSWORD='<cloud-password>'
./mvnw spring-boot:run
```

## Scenario 2: Edge only (offline / no internet)

Use when the school has no internet connection.

Behavior:

- Run backend with `edge` role.
- All operations are local to edge PostgreSQL.
- No push/pull to cloud.

Recommended settings:

- `SPRING_PROFILES_ACTIVE=edge`
- `app.runtime.scenario=EDGE_ONLY`
- `app.sync.edge.cloud-base-url` unset/empty
- `app.sync.edge.worker-enabled=false` (recommended in strict offline mode)

Example:

```bash
cd core-backend
export SPRING_PROFILES_ACTIVE=edge
export APP_RUNTIME_SCENARIO=EDGE_ONLY
export ZIVAI_DB_URL='jdbc:postgresql://localhost:5432/zivai_edge'
export ZIVAI_DB_USERNAME='zivai'
export ZIVAI_DB_PASSWORD='<edge-password>'
unset ZIVAI_CLOUD_SYNC_BASE_URL
export APP_SYNC_EDGE_WORKER_ENABLED=false
./mvnw spring-boot:run
```

## Scenario 3: Edge + Cloud (hybrid / poor internet)

Use when the school has intermittent internet and needs offline continuity with eventual sync.

Behavior:

- Cloud process runs with `cloud` role.
- School-local process runs with `edge` role.
- Edge writes local data and syncs changes using push/pull when connectivity is available.

Recommended settings:

- Cloud:
  - `SPRING_PROFILES_ACTIVE=cloud`
- Edge:
  - `SPRING_PROFILES_ACTIVE=edge`
  - `app.runtime.scenario=HYBRID` (or `AUTO` with cloud URL set)
  - `app.sync.edge.cloud-base-url` set
  - `app.sync.edge.worker-enabled=true`

Cloud start:

```bash
cd core-backend
export SPRING_PROFILES_ACTIVE=cloud
export ZIVAI_DB_URL='jdbc:postgresql://<cloud-host>:5432/<cloud-db>'
export ZIVAI_DB_USERNAME='<cloud-user>'
export ZIVAI_DB_PASSWORD='<cloud-password>'
./mvnw spring-boot:run
```

Edge start:

```bash
cd core-backend
export SPRING_PROFILES_ACTIVE=edge
export APP_RUNTIME_SCENARIO=HYBRID
export ZIVAI_DB_URL='jdbc:postgresql://localhost:5432/zivai_edge'
export ZIVAI_DB_USERNAME='zivai'
export ZIVAI_DB_PASSWORD='<edge-password>'
export ZIVAI_CLOUD_SYNC_BASE_URL='http://<cloud-host>:5000'
export ZIVAI_EDGE_NODE_ID='<edge-node-uuid>'
export ZIVAI_EDGE_AUTH_KEY='<shared-secret>'
export APP_SYNC_EDGE_WORKER_ENABLED=true
./mvnw spring-boot:run
```

## Auto-scenario behavior

If `app.runtime.scenario=AUTO`:

- Cloud role resolves to `CLOUD_ONLY`.
- Edge role resolves to:
  - `HYBRID` when `app.sync.edge.cloud-base-url` is set
  - `EDGE_ONLY` when `app.sync.edge.cloud-base-url` is empty

## Sync endpoints

Cloud role:

- `POST /api/sync/push`
- `GET /api/sync/pull`

Edge role:

- `GET /api/sync/edge/status`
- `POST /api/sync/edge/run`

## Common startup issues

1. DB auth error (`SCRAM ... no password provided`)
- Set `ZIVAI_DB_PASSWORD` correctly for the target DB user.

2. Role/profile mismatch
- Do not combine `edge` and `cloud` profiles in one process.
- Ensure `app.runtime.scenario` is compatible with the selected role.

3. HYBRID edge fails at boot
- Set `app.sync.edge.cloud-base-url` (or `ZIVAI_CLOUD_SYNC_BASE_URL`) to a non-empty URL.
