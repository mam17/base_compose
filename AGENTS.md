# AGENTS

Android jigsaw puzzle game (`md018_puzzle_game`), written in Kotlin + Jetpack Compose.

## Mandatory before any task

1. Read and follow `RULES.md` at the repo root: language, honesty, git, scope of changes, test quality, security. It takes precedence over this file.
2. Read `CONTEXT.md` at the repo root: stack, modules, Gradle and verify commands, base classes, naming conventions, domain glossary. Use its terms and commands exactly.

## Skills

The skill set lives in `.agents/skills/` (Codex and Gemini CLI read it directly). Claude Code reads `.claude/skills/` and Antigravity reads `.agent/skills/`; both are junctions created by `.agents/link-skills.ps1`. A skill is missing → run `powershell -ExecutionPolicy Bypass -File .agents\link-skills.ps1`.

- `ship-feature` (only when the user invokes it): full pipeline from request to handover, 3 approval gates.
- `android-context-setup` (only when the user invokes it): update `CONTEXT.md`, `AGENTS.md`, `CLAUDE.md`, verify scripts.
- Use standalone when relevant: `android-grill`, `android-plan`, `android-spec`, `android-test-design`, `android-tickets`, `android-tdd`, `android-self-test`, `android-diagnose`, `android-review`, `android-deliver`.

Agents without skill support: still follow `RULES.md` and the rules below.

## Change tracking (when working on a feature in `docs/features/<slug>/`)

- Every created/edited file: add its relative path (with `/`) to `docs/features/<slug>/changed-files.txt`, one per line, no duplicates.
- Before the first edit of an EXISTING file: back up the original to `.agent-backup/<slug>/<path>`. Never overwrite an existing backup.
- Undo = copy back from `.agent-backup/<slug>/`; newly created files are deleted. Never use git to undo your own changes.

## Definition of DONE

Work is done only when the verify script (Commands section of `CONTEXT.md`) exits with code 0 AND every test case in `test-matrix.md` (if any) is ✅ or ⏭️ with a reason.

## Project rules (in addition to `RULES.md`)

- New dependency: in the pipeline it must be listed in `plan.md` and approved at GATE 2; outside it, ask the user.
- Never hardcode `Dispatchers.*` in new code; always inject.
- Never run instrumented tests (`connected...AndroidTest`) inside the TDD loop.
- Editing a file outside the plan → record the reason in `progress.md`.
- Found a new term or convention → propose updating `CONTEXT.md`, not this file.
