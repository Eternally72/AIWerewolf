# Agent Design

每个 AI 玩家是独立 Agent，拥有独立身份、阵营、私有记忆、策略和发言风格。

## LLM

- `LlmGateway`：统一模型网关，负责根据 `llm.provider` 选择模型 Provider，并把 provider、模型名、耗时、fallback 原因返回给 `AgentRun`。
- `ModelProvider`：模型供应商抽象，目前包含 `MockModelProvider`、`BailianModelProvider`、`DeepSeekModelProvider`、`ZhipuModelProvider` 和 `GenericOpenAiCompatibleModelProvider`。
- `MockModelProvider`：默认启用，不调用真实模型，CI 可运行。
- `OpenAiCompatibleProviderSupport`：复用 `/chat/completions` 协议、响应解析和可用性校验。
- `BailianModelProvider`：用于阿里百炼 OpenAI-compatible 接口，通过 `BAILIAN_*` 环境变量启用。
- `DeepSeekModelProvider`：用于 DeepSeek OpenAI-compatible 接口，通过 `DEEPSEEK_*` 环境变量启用。
- `ZhipuModelProvider`：用于智谱 OpenAI-compatible 接口，通过 `ZHIPU_*` 环境变量启用。
- `GenericOpenAiCompatibleModelProvider`：用于任意兼容 `/chat/completions` 的平台，通过 `OPENAI_COMPATIBLE_*` 环境变量启用。

模型网关不会把 API Key、完整请求头或敏感连接信息写入日志或 `AgentRun`。当配置的 provider 未注册、不可用或调用失败时，网关会透明回退到 Mock Provider，并在 `AgentRun.errorMessage` 中记录脱敏后的 fallback 原因。

## Prompt 设计

Prompt 由 `PromptRegistry` 统一加载、缓存和版本化。主链路不再在 Java 代码里硬编码任务 Prompt。

角色系统提示词位置：

```text
server/src/main/resources/prompts/roles/
├── common.md
├── unknown.md
├── werewolf.md
├── wolf_king.md
├── white_wolf_king.md
├── hidden_wolf.md
├── villager.md
├── seer.md
├── witch.md
├── hunter.md
├── guard.md
├── idiot.md
├── knight.md
├── grave_keeper.md
├── magician.md
├── cupid.md
└── elder.md
```

`common.md` 定义所有 Agent 共享的硬约束：信息隔离、禁止上帝视角、JSON 输出格式、fallback 行为。每个角色文件只描述该角色的可见信息、胜利目标、夜晚策略、白天发言、投票和技能使用方式。

任务 Prompt 位置：

```text
server/src/main/resources/prompts/tasks/
├── speech.md
├── vote.md
├── night-action.md
└── day-skill.md
```

输出 Schema 位置：

```text
server/src/main/resources/prompts/output-schema/
├── speech.schema.json
├── vote.schema.json
├── night-action.schema.json
└── day-skill.schema.json
```

`PromptRegistry` 会在启动时加载并缓存所有角色 prompt、任务 prompt 和输出 schema。角色文件名由 `Role.name().toLowerCase()` 决定，例如 `WHITE_WOLF_KING` 对应 `white_wolf_king.md`。如果新增角色或任务但没有新增对应资源文件，Registry 会抛出异常，测试也会失败。

## 输出格式

AI 发言、投票、夜间行动和白天技能均使用 JSON。`AiAgentService` 会调用 `LlmGateway` 选择的模型 Provider，提取返回文本中的 JSON 对象，校验目标是否合法，再转换为领域决策对象。

非法 JSON、缺失字段、非法目标、狼人投可见狼队友等情况会触发 fallback。Mock LLM 默认返回 JSON，本地和 CI 不需要真实 API Key。

## AgentRun 追踪

每一次 AI 发言、投票、夜间行动和白天技能都会记录为一条 `AgentRun`。记录内容包括：

- 房间、玩家、轮次、阶段和决策目的。
- 输入给 Agent 的过滤后 `GameView` 快照。
- Prompt 版本、任务 Prompt 版本、模型 Provider 和模型名。
- 原始模型输出、解析后的领域决策、fallback 状态。
- 调用尝试次数、耗时和错误摘要。

当前版本格式示例：

- `promptVersion`: `role-prompts-v1:SEER`
- `taskPromptVersion`: `task-prompts-v1:VOTE/output-schema-v1:VOTE`

`AgentRun` 可能包含玩家私有视角和策略信息，因此只能通过 GodView 令牌访问：

```http
GET /api/rooms/{roomId}/agent-runs
X-God-View-Token: <godViewToken>
```

Agent Prompt 会拼接 Redis 中保存的短期记忆：

```text
agent:{roomId}:{playerId}:stm
```

短期记忆只来自该 Agent 可见的私有/共享记忆，不包含 GodView。

## Fallback

当 AI 决策为空、目标非法或解析失败时，系统选择第一个合法存活目标或 `NONE`，确保自动对局不中断。

## Evaluation

`EvaluationRunner` 用于批量运行全 AI 7 人标准局，并生成 Agent 系统稳定性报告。当前指标包括：

- 完整结束率和失败局数。
- `AgentRun` 总数。
- fallback 次数和 fallback 率。
- 非法决策 fallback 次数。
- 公共视角泄露计数。
- 平均 Agent 决策延迟。
- 平均游戏轮次。

评测入口：

```http
POST /api/evaluations/run
```

该接口仅在 `dev/test` profile 暴露。当前只支持 `7-standard` 模板，单次请求最多运行 20 局。

## Observability

第六阶段接入 Spring Boot Actuator + Micrometer + Prometheus，把 Agent 主链路从“可记录”升级为“可观测”。核心实现位于：

```text
server/src/main/java/com/example/aiwerewolf/aiinfra/observability/
├── AiInfraMetrics.java
└── AiInfraObservation.java
```

当前已埋点的路径：

- `LlmGateway`：统计模型调用次数、fallback 次数、provider 尝试次数和网关耗时。
- `AgentRunService`：统计已落库的 Agent 决策运行次数、fallback 次数和决策耗时。
- `AgentTaskService`：统计 Agent Worker 任务排队、运行、成功、失败、超时状态和任务耗时。
- `GamePhaseEngine`：统计阶段推进成功/失败次数和推进耗时。
- `EvaluationRunner`：统计评测套件次数、完成局数、失败局数、泄露计数、fallback rate 和评测耗时。

主要指标名：

```text
aiwerewolf.llm.calls
aiwerewolf.llm.fallbacks
aiwerewolf.llm.attempts
aiwerewolf.llm.latency
aiwerewolf.agent.runs
aiwerewolf.agent.fallbacks
aiwerewolf.agent.run.latency
aiwerewolf.agent.tasks
aiwerewolf.agent.task.latency
aiwerewolf.game.phase.advances
aiwerewolf.game.phase.advance.latency
aiwerewolf.evaluation.runs
aiwerewolf.evaluation.completed.games
aiwerewolf.evaluation.failed.games
aiwerewolf.evaluation.leakage
aiwerewolf.evaluation.fallback.rate
aiwerewolf.evaluation.duration
```

这些指标会带上低基数标签，例如 `purpose`、`provider`、`model`、`fallback`、`phase`、`status`、`template`。不要把玩家 ID、房间 ID、原始 Prompt、模型输出或 API Key 作为指标标签，避免高基数和隐私泄露。

`AiInfraObservation` 提供 Micrometer Observation 包装层，目前用于模型网关调用。后续接入 OpenTelemetry/Zipkin/Tempo 时，可以在不重写业务代码的前提下扩展 trace 导出。

## Agent Task

`AgentTaskService` 负责记录每次 Agent 决策的任务生命周期。为了保证狼人杀公开发言上下文严格有序，当前游戏主流程采用“单 Agent、单步骤、同步执行”模式：

```text
GamePhaseEngine
  -> SpeechService / VoteService / NightActionService
  -> AgentTaskService.execute(...)
  -> AiAgentService
  -> LlmGateway
```

每次 `advance` 最多调用一个 Agent。当前 Agent 的发言、投票或行动持久化后，下一次推进才会构建后置 Agent 的私有视角。该约束避免后置 Agent 读取到空的 `speeches`，也防止多个模型并发决策时基于同一份过期上下文。

典型流程：

```text
1. 查询当前阶段按座位排序的待行动玩家。
2. 如果当前轮到真人，暂停并等待前端提交。
3. 如果当前轮到 AI，在当前推进线程中构建过滤后的私有视角。
4. 同步调用 `AiAgentService` 并写入 Speech / Vote / GameAction。
5. 当前阶段全部玩家完成后才进入阶段结算。
```

任务状态包括：

```text
QUEUED -> RUNNING -> SUCCEEDED
QUEUED -> RUNNING -> FAILED
```

最近 100 条房间任务可通过 GodView 令牌查询：

```http
GET /api/rooms/{roomId}/agent-tasks
X-God-View-Token: <godViewToken>
```

`AgentTask` 只记录任务调度元信息，例如任务 ID、房间、玩家、轮次、阶段、用途、状态和耗时；具体模型输入、输出、Prompt 版本和 fallback 原因仍由 `AgentRun` 负责记录。

当前版本已经将 `AgentTask` 状态落库到 `agent_tasks` 表，服务重启后仍可通过 GodView 查询最近任务。模型 HTTP 超时由各 Provider 的客户端超时配置控制；调用失败时由模型网关和 Agent fallback 保证游戏主流程继续运行。

## GameEvent Replay

`MemoryService` 在追加公共、私有、共享秘密和上帝视角记忆时，会同步写入 `game_events` 表。这样阶段变化、发言、投票、死亡、身份分配、AI 策略摘要等都可以进入回放流。

回放接口：

```http
GET /api/rooms/{roomId}/replay/public
GET /api/rooms/{roomId}/replay/god
X-God-View-Token: <godViewToken>
```

公开回放只返回 `PUBLIC` 事件；GodView 回放返回完整事件，包含私有技能结果、狼队共享信息和上帝视角事件。
