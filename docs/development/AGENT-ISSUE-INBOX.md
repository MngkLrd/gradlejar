# Agent Issue Inbox — durable cross-agent requests

Under [AGENTS.md](../../AGENTS.md), GitHub Issues are the durable work queue for MineMods developers. The protocol is mandatory for active agents handling their assigned module or infrastructure, especially first-party ecosystems. It is intentionally small: one scoped inbox lookup at the beginning of a working iteration, then act on the relevant results. Do not rely on another chat's memory, chat-to-chat messages, notifications, or the owner repeating requests.

## 1. Start of a working iteration — one quick inbox lookup

A working iteration is an implementation, fix, integration, or delivery continuation for an assigned product/subsystem. It is not every tool call, compile, CI poll, status-only question, or asset draft.

1. Resolve your stable module identity from the actual project/integration manifest, plus its existing human-facing name and known aliases (for example WorldLogic Core, Trauma, Hydration, Temperature/Thermal). Do not use the JAR version or branch name as the identity. Standalone mods use their established project identity; infrastructure work uses its actual assigned component.
2. Fetch open GitHub Issues in MngkLrd/gradlejar once, excluding pull requests. In a small repository inbox use one list/search: `repo:MngkLrd/gradlejar is:issue is:open`. Inspect titles and available short body previews locally; a matching name alone is not proof of responsibility. Check actual recipient and ownership, not merely the author or every module mentioned.
3. For a larger backlog use a targeted recipient search, e.g. `repo:MngkLrd/gradlejar is:issue is:open "worldlogic_hydration" in:title` (illustrative ID; substitute the actual stable identity). Verify exact recipient token in returned titles. Search by established aliases as needed for legacy issues while they remain open; the small-inbox list is often cheaper than several searches. Do not assume GitHub indexing, punctuation matching or result limits guarantee completeness. Follow pagination or use a direct open-issue listing if the relevant results are truncated or search is unavailable. Do not scan closed history or unrelated PRs by default.
4. Open full details/comments only for relevant or genuinely ambiguous results. Reuse this inbox snapshot and issue IDs for the rest of the iteration. No repeated broad lookup after each build or edit. If none are addressed to you, proceed immediately; do not generate an empty report. If the connector/search fails, distinguish UNKNOWN from no requests, continue supported work, and report the limitation if it affects delivery.

Existing issue titles are heterogeneous: e.g. `[Hydration → Metabolism]`, `WorldLogic Core / Terrarian`, and prose descriptions. Read their actual requested action, including body where necessary. An arrow or slash in a legacy title does not reliably identify the recipient. No mass relabeling or rewriting historical issues is required. New routing format below is adopted on creation or when an existing issue is actually touched.

## 2. Create and route a request

Before filing, check the current inbox results, known issue links and relevant provider source/PR. Search for a similar open request only if the existing snapshot is insufficient. Update the existing issue when the same requirement is already recorded; do not create a duplicate for every API field. Use one actionable request per responsible owner or coherent domain; separate independently deliverable owners only when helpful.

New issue title format: `[to:STABLE_MODULE_ID] Short actionable outcome`. The ID comes from the actual project/manifest; use the stable established ID, not an invented mapping. Multiple recipients may use separate `[to:ID]` tokens when one joint acceptance criterion genuinely requires them; prefer one owner and linked follow-up requests when work can finish independently. Existing labels may optionally assist routing if they actually exist; labels and GitHub account assignees are not required and cannot distinguish chat agents sharing an account.

Compact body (adapt to task; no separate Markdown assignment):
- Requester: stable module identity; Recipient: stable module identity.
- Need: concrete gameplay/technical result, affected target and relevant issue/PR/source links.
- Contract (where applicable): capability and actual inputs/outputs, units, lifecycle, side/thread, ownership, compatibility and safe fallback.
- Acceptance: observable success and negative/boundary checks relevant to the request.
- Blocker/priority: what work is blocked, if any; identify dependency and next action.

For ecosystem API requests follow [ECOSYSTEM-INTEGRATION.md](ECOSYSTEM-INTEGRATION.md). Avoid inventing provider APIs, duplicating domain state, or assigning a task to an unrelated owner. Do not silently substitute a chat note for a necessary Issue. Link issue IDs from relevant PRs/manifests as appropriate.

## 3. Triage and execute

| Actual state | Action by responsible agent |
| --- | --- |
| Open, addressed to you, already completed | Verify real source/target and required evidence; post concise closure evidence and close immediately. Do not rebuild solely because an Issue was left open if matching evidence already exists. |
| Open, actionable and within your product authority | Implement now as part of the current iteration; include in existing targeted QA/build where compatible. Fix integration blockers first. Do not just acknowledge, copy into another chat or defer a small compatible request by default. |
| Open, implementation already in progress | Reuse/coordinate with the actual branch or PR; do not create a competing implementation. Track its evidence and close when done. |
| Open, dependent on another owner or missing evidence | Leave open; link the actual blocking issue/PR and describe precise remaining action. Continue independent work. |
| Open, genuine design conflict, major unrelated scope, incompatible target or permissions | Leave open with a concise reason and next action; resolve through the proper owner/PR or request a decision only when necessary. Do not silently ignore or expand the release scope. |
| Duplicate | Link the canonical still-tracked issue, explain equivalence, close as duplicate if it truly is the same task; never discard a distinct unmet requirement. |
| Not planned / explicitly superseded | Close with the appropriate non-completed reason only when grounded in an actual owner decision or equivalent authorized replacement; link the decision/replacement. |

A provider owns its own implementation, a consumer owns its integration. Check actual relevant peer contracts and coordinate instead of editing the other owner's product without authority. If a provider request and consumer integration have distinct acceptance criteria, track them separately: provider may close its fulfilled API request without waiting for unrelated consumer work, but the consumer integration remains open until its own criteria pass. For a joint issue, record completed parts and leave open until every required part is done or explicitly split with linked issues. Do not close another owner's unrelated task.

Prioritize user-requested work and actual integration blockers; handle small compatible requests in the same scoped implementation rather than accumulating a second backlog. A large conflicting request stays visible with evidence and next action, not silently dropped or treated as a release waiver. No automatic multi-version expansion, mass migration, additional runtime tests for unrelated products, or new build solely for an already verified completion.

## 4. Close the loop — evidence, not promises

At the end of the iteration reconcile only issues you touched or relevant previously completed issues identified in the inbox. For each completed request, verify its current state, actual implemented acceptance criteria, exact target/ref and relevant passing QA (or genuine non-applicability). A source change, planned API, queued CI, compilation alone for gameplay, or another agent's unverified statement is not completion.

Use a short GitHub comment:
- Delivered: what changed, actual target, API revision/signature if applicable.
- Evidence: commit/PR, exact CI run and artifact/version if needed; mention limitations.
- Consumer handoff: how to use it or linked remaining consumer task, if relevant.

Then actually set the issue to closed/completed; verify the returned state. For provider delivery, normally require merged/available verified implementation on the agreed target; an explicitly agreed verified branch artifact is acceptable if the recipient can actually consume it and the handoff contract does not require main/release. If required CI is pending/failed, artifact inaccessible, or merge required but missing, leave open with the real link and status. Recheck that particular issue on the next continuation, not the entire repository while waiting. Prefer explicit evidence-based closure to automatic closing keywords that could close a request before required checks pass. If a PR already auto-closed it prematurely, reopen with the actual blocker; never call incomplete work complete.

For previously finished but still open requests, use the same closure rule with existing exact evidence. No repeated implementation or duplicate build is needed. If a regression reopens a fulfilled need, reopen the original issue with new evidence or link a distinct regression issue where appropriate. Avoid repetitive comments when state and blocker have not changed. Do not close requests merely to keep the inbox empty.

## 5. Cost and operational boundaries

Normal overhead: one inbox lookup per active working iteration, one local title/recipient filter, detail reads for matching requests only, and comments/closure writes only for changed tasks. No periodic whole-repository audit, repeated polling, additional CI stage, all-module report, mandatory external service or separate project-management board. For status-only requests inspect the named PR/issue/run directly, not the entire inbox. Preserve all existing product QA and release gates.

GitHub is the durable source of truth, not a guarantee of autonomous background work. An inactive chat agent cannot receive or execute a request by itself; the inbox is processed when the responsible agent is active. The contract does not install an issue watcher, auto-close bot or scheduler. If a request cannot be executed now, keep it open with accurate evidence so the next responsible continuation can recover it. The owner should not have to relay routine handoffs, operate the issue queue, or repeatedly remind agents to close finished tasks.
