# AI-AGENT

AI-AGENT is the control plane for a closed-loop autonomous development system:

`Goal → Task → Google Antigravity → Result → Evaluation → next Task / DONE`

## What this repository does

- Stores machine-readable tasks and results.
- Runs a Google Antigravity agent in GitHub Actions, so a local PC is not required.
- Uses the official Google Antigravity Python SDK.
- Keeps execution isolated inside the GitHub Actions runner.
- Produces structured JSON results for a future supervisor/evaluator.

## Important authentication note

Google's Antigravity CLI currently uses Google/OAuth-style authentication for its own headless workflow. A Gemini API key is **not currently supported by the Antigravity CLI**. For automation, this repository therefore uses the official **Antigravity SDK**, which supports `GEMINI_API_KEY`.

Create a Gemini API key in Google AI Studio, then add it to this repository as the GitHub Actions secret:

`Settings → Secrets and variables → Actions → New repository secret`

Name it:

`GEMINI_API_KEY`

Never commit the key into this repository.

## First test

1. Add `GEMINI_API_KEY` as a repository secret.
2. Create `tasks/inbox/example.json` from the template in `tasks/TASK_TEMPLATE.json`.
3. Commit it to `main`.
4. GitHub Actions starts the Antigravity worker.
5. The worker writes the result to `tasks/results/`.

## Architecture

```text
You / Supervisor
      ↓
 tasks/inbox/*.json
      ↓
 GitHub Actions
      ↓
 Antigravity SDK
      ↓
 repository workspace
      ↓
 tasks/results/*.json
      ↓
 Supervisor evaluates result
      ↓
 DONE or another task
```

## Security

The first version is deliberately conservative. The worker runs inside GitHub's ephemeral runner and uses a restricted task protocol. Do not put production credentials in task files. Do not use unrestricted permission modes until the task protocol and repository protections have been reviewed.
