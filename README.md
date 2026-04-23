# Metis

Metis is a coding skill for LLMs.

It is for implementation, refactoring, and code review work where you want the model to lean toward:

- plain-data domain models
- top-down API design
- explicit control flow
- boundary assertions
- behavior-focused testing
- early performance thinking

The goal is not rigid dogma. The goal is better engineering judgment.

## Install

Before publishing, test it from a local checkout:

```bash
npx skills add . --skill metis -g
```

Target a specific agent if you want:

```bash
npx skills add . --skill metis -g -a codex
npx skills add . --skill metis -g -a claude-code
```

If symlinks are blocked on Windows, use `--copy`.

```bash
npx skills add . --skill metis -g -a codex --copy
```

After you push the repo, the same command works with the GitHub URL instead of `.`:

```bash
npx skills add https://github.com/gitRasheed/metis-skill --skill metis -g
```

## Use

In Codex, invoke it as `$metis`.

In Claude Code, the standalone skill is available as `/metis` when the repo is open as a project, or when `.claude/skills/metis/` is copied into your global Claude skills directory.

## Codex plugin

This repo also includes a Codex plugin wrapper and marketplace catalog:

- `.codex-plugin/plugin.json`
- `.agents/plugins/marketplace.json`

That gives you a native Codex plugin path in addition to the simpler `npx skills add` path.

## Structure

```text
metis-skill/
├── .agents/plugins/marketplace.json
├── .claude/skills/metis/SKILL.md
├── .codex-plugin/plugin.json
├── skills/metis/SKILL.md
├── skills/metis/agents/openai.yaml
├── skills/metis/references/
└── PORTABLE_PROMPT.md
```

## Notes

- `skills/metis/` is the main skill.
- `.claude/skills/metis/` is the standalone Claude version.
- `PORTABLE_PROMPT.md` is the plain markdown version for other agents.
