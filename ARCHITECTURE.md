# Core Backend Runtime Layout

## Package rule

- `zw.co.zivai.core_backend.common`
  - shared LMS/domain logic used by both runtime roles
  - controllers, configs, dtos, exceptions, models, repositories, services, websockets, shared aspects
- `zw.co.zivai.core_backend.edge`
  - edge-only sync capture, sync worker, edge sync endpoints
- `zw.co.zivai.core_backend.cloud`
  - cloud-only sync endpoints and cloud-side sync orchestration

## Runtime roles

- `edge`
  - school-local backend
  - supports offline operation
- `cloud`
  - central backend
  - accepts sync pushes and serves pull feeds

## Deployment scenarios

- `CLOUD_ONLY`
  - `SPRING_PROFILES_ACTIVE=cloud`
- `EDGE_ONLY`
  - `SPRING_PROFILES_ACTIVE=edge`
  - do not set `app.sync.edge.cloud-base-url`
- `HYBRID`
  - `SPRING_PROFILES_ACTIVE=edge`
  - set `app.sync.edge.cloud-base-url`

`app.runtime.scenario=AUTO` derives the scenario from the active role and sync target.

## Scenario guide

Detailed setup and run instructions for:

- cloud-only (good internet)
- edge-only/offline (no internet)
- hybrid edge+cloud (poor/intermittent internet)

See `core-backend/docs/deployment-scenarios.md`.
