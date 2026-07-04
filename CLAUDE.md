# CLAUDE.md — Workflow Approval System

## Quick Facts
- Monorepo: `backend/` (Spring Boot 3.5, Java 17, Maven), `frontend/` (Next.js 16 App Router, React 19, TypeScript), `agent/` (Python governed AI review agent, uv)
- Backend: `cd backend && ./mvnw test` / `./mvnw compile`; local infra via `backend/compose.yaml`
- Frontend: `cd frontend && npm run lint` / `npm run build` / `npm test` (jest); i18n key check: `npm run check:i18n`
- Branching: `develop` is integration; one `feature/*` or `fix/*` branch per task; PR into `develop`; `main` is release
- Conversation with the user is in Traditional Chinese; all code, comments, tests, and docs are in English

## Architecture Map
- Backend root package: `com.eva.workflow.approval` — REST API, JWT auth via HttpOnly cookie (never localStorage)
- Approval chain: DRAFT → PENDING → DEPUTY (step 1) → DIRECT_MANAGER → DEPARTMENT_MANAGER (only if ≥ 4 days) → APPROVED / REJECTED
- AI leave review: two-stage Policy RAG (rule filter → policy passage retrieval); exposed as `workflow` MCP tools (`get_request_context` / `evaluate_rules` / `search_policy`); the governed agent lives in `agent/`
- Frontend service layers: `BaseService → ApiService → DomainService`; pages call DomainService only
- Form pages use Zustand with `getState()` in event handlers (not `useStore()` subscriptions) to avoid Radix dismissable-layer loops; mutation-identifying data goes in `mutate({...})` variables, not closures

### Domain Glossary
| Term | Meaning |
| --- | --- |
| DEPUTY | Approval step 1 — the deputy IS the first approver, not a bypass |
| DIRECT_MANAGER / DEPARTMENT_MANAGER | Approval steps 2 and 3; step 3 applies only to requests ≥ 4 days |
| adjust-balance | Admin flow for correcting leave balances (Phase 2) |
| G-A5 | Unsaved-changes guard spec item #5 |
| Policy RAG | Two-stage retrieval: rule filter first, then policy passage retrieval for citations |
| 2A / 2B / 2D | Phase 2 roadmap milestone codes (see `docs/plans/`) |

## Git Workflow (IMPORTANT)
- **Checkpoint commits**: after each completed logical unit (one endpoint, one component, one test group), commit on the feature branch **without asking**. Aim for ≤ ~200 diff lines per commit where practical.
- **Gate per commit**: related tests + compile pass (e.g. `./mvnw test -Dtest=XxxTest`, `npx tsc --noEmit`). The full suite is NOT required per commit.
- **Before opening a PR**: run the full test suites + lint, then run /code-review and fix confirmed findings.
- **PR description**: include a commit-by-commit reading guide ordered by risk (which commits carry core logic vs boilerplate/tests).
- The user's review happens at PR merge — never ask permission for checkpoint commits; never merge without the user.
- **Commit messages**: conventional style (`feat:`/`fix:`/`docs:`/...). Never add AI attribution trailers (`Co-Authored-By: Claude`, `Generated with ...`) — commits are authored solely by the user.
- **Plan documents** live in `docs/plans/` and are committed together with the work they describe; superseded plans move to `docs/plans/archive/` instead of being deleted.

## Model Policy
- Planning wants the strongest available model; execution may use a faster one. A PreToolUse hook on EnterPlanMode reminds about `/model` — do not remove it.

## Session Rituals
- Session start: `/kickoff` — rebuild context from progress memory + git state before doing work.
- Session end: `/wrap-up` — checkpoint remaining work, push, record progress + RESUME instructions.
