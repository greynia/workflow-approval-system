# 請假審核系統（Workflow Approval System）

[English](README.md) | 繁體中文

一個帶有「受治理 AI 審核層」的請假簽核平台。員工送出請假申請、主管簽核，AI 審核員產出一份風險評估
供主管參考。AI 只負責建議——風險底線由確定性規則引擎掌握——並將同一組受治理的後端工具透過
**Model Context Protocol (MCP)** 開放給 AI agent 使用。

**技術棧：** Java 17 · Spring Boot 3.5 · PostgreSQL 16 + pgvector · Next.js 16 / React 19 ·
Python (MCP) · Gemini · Ollama · Jina Rerank

## 功能

- **請假簽核流程**——多層簽核鏈、代理人與抽單流程、請假額度與假日行事曆處理、角色權限
  （員工／主管／管理者）與操作稽核日誌。
- **AI 審核管線**——確定性規則引擎標記風險並設定底線；以 RAG 檢索相關公司規章；LLM 產出附政策
  引用的摘要、風險等級與建議。
- **確定性安全護欄**——最終風險為 `max(LLM, 規則底線)`，模型只能往上加風險、無法調降；高風險不
  自動核准；最終決策保留給人類主管。
- **以 MCP 將工具開放給 AI agent**——受治理的後端工具以 MCP server 發布，並透過 Skill 與最小權限
  Subagent 接進 Claude Code。agent 的每次呼叫都以專用 `AI_AGENT` 服務帳號執行並留稽核。
- **韌性與可觀測性**——多 provider fallback（Gemini → Ollama → 純規則）；逐筆記錄 provider、token
  用量、延遲、失敗與 prompt 版本。

## 架構

```
Next.js 前端
      │  REST + JWT（httpOnly cookie）
Spring Boot 後端   — 六角架構：api / application / domain / infrastructure
  • 規則引擎 + Policy RAG（pgvector）+ LLM 審核 + 確定性 floor
  • 受治理、唯讀的 AI 端點（限 AI_AGENT / ADMIN，全程稽核）：
        GET /api/ai/requests/{id}/context
        GET /api/ai/requests/{id}/rule-evaluation      ← 權威風險底線
        GET /api/ai/policy-search
      ▲
      │  HTTP（以 ai-agent@system.local 登入）
MCP server（Python / FastMCP）   — 3 個工具，薄協定轉接層
      ▲
      │  MCP（stdio）
Claude Code   — leave-review Skill + leave-reviewer Subagent
```

## AI 審核流程

請假單建立後，會跑一條非同步管線：

1. **組裝**申請人與請假脈絡。
2. **規則引擎**產出 hard-rule flags 與風險底線（如頻繁請假、新進員工長假、長天數）。
3. **Policy RAG**（命中 flag 才跑）——查詢向量化（`gemini-embedding-001`，768 維）→ pgvector
   餘弦搜尋 → 可選 Jina rerank → 取前 k 條規章。
4. **LLM** 依檢索到的規章產出摘要／風險／建議／引用。
5. **夾制底線**——`final = max(LLM, floor)`；將 `HIGH + APPROVE` 降為 `REVIEW_CAREFULLY`。
6. **落地保存**並附完整 provenance。

LLM 不可用時，審核降級為純規則結果，簽核流程不會卡在 AI 可用性上。

## 將工具接給 AI agent（MCP）

上述三個唯讀端點由 MCP server（`agent/`）包裝，任何支援 MCP 的 agent 都能當工具呼叫：

| 檔案 | 用途 |
|---|---|
| `agent/src/agent/mcp_server.py` | MCP server（FastMCP），對外提供 `evaluate_rules` / `get_request_context` / `search_policy` |
| `.mcp.json` | 把 server 註冊進 Claude Code（用 `uv` 跨平台） |
| `.claude/skills/leave-review/SKILL.md` | 描述受治理審核流程的 Skill |
| `.claude/agents/leave-reviewer.md` | 採最小權限工具 allowlist 的 Subagent |

MCP server 是薄轉接層、不含任何業務邏輯——規則引擎與權威風險底線只存在於後端。

## 技術棧

**後端** — Java 17、Spring Boot 3.5（Web、Security、Data JPA、Validation、Actuator、Cache）、
JWT（httpOnly cookie）+ refresh token、Flyway、PostgreSQL 16 + pgvector（hibernate-vector）、
langchain4j（Gemini、Jina rerank）、Caffeine、MapStruct、Lombok、springdoc OpenAPI。

**前端** — Next.js 16、React 19、TypeScript、Tailwind CSS、Zustand、TanStack Query、
React Hook Form + Zod、next-intl（en / zh-TW）、Storybook。

**Agent** — Python ≥ 3.11、MCP（FastMCP）、pydantic、httpx、LangGraph；以 `uv` 管理。

**AI providers** — Gemini（`gemini-2.5-flash-lite` 審核、`gemini-embedding-001` embedding）、
Ollama（`llama3.1:8b`，本地 fallback）、Jina（`jina-reranker-v2-base-multilingual`）。

## 快速開始

```bash
# 1. 設定——複製範例檔並填入 DB 帳密（與選用的 Gemini key）
cp backend/.env.example backend/.env
cp backend/src/main/resources/application-local.properties.example \
   backend/src/main/resources/application-local.properties

# 2. 資料庫（PostgreSQL 16 + pgvector）
cd backend && docker compose up -d

# 3. 後端（啟動時自動跑 Flyway migration）
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 4. 前端
cd frontend && npm install && npm run dev
```

若要從 Claude Code 驅動這些工具，安裝 [`uv`](https://docs.astral.sh/uv/)、同步 agent 環境並核准
MCP server：

```bash
uv --directory agent sync
cp agent/.env.example agent/.env       # 設定 AGENT_PASSWORD
claude                                 # 從 repo root 開，用 /mcp 核准 "workflow"
```

## Roadmap

- 以 LangGraph 自建 programmatic agent，使用同一組 MCP 工具。
- Agent 執行鏈追蹤與 token／成本歸戶。
- 場景化的 guardrail 評測套件。
- 線上 demo。
