# Google Antigravity in AI-AGENT

## The three useful ways to run it

### 1. Antigravity CLI in GitHub Actions

Google's current CLI has a non-interactive `-p/--print` mode and machine-readable `json` / `stream-json` output. It can run in CI/headless environments. The CLI also supports Gemini API-key authentication when `modelProvider` is set to `gemini` in its settings.

### 2. Antigravity Python SDK

This repository currently uses this option.

```bash
pip install google-antigravity
```

The SDK uses the same Antigravity agent harness and can be configured with `GEMINI_API_KEY`. It exposes a Python `Agent` API and handles the local runtime, tool execution, policies, sessions and structured output.

### 3. Managed Antigravity Agent API

Google also exposes Antigravity as a managed agent through the Gemini Enterprise Agent Platform. This is the strongest long-term option for a completely cloud-native architecture because the agent can run in a remote environment and be invoked through the Interactions API.

The managed API uses a Google Cloud project and supports the first-party Antigravity base agent. This can eventually remove the need to install the Antigravity runtime inside GitHub Actions.

## Which one AI-AGENT uses now

Version 1 uses **the official Python SDK inside GitHub Actions**.

Why:

- no local computer is required;
- GitHub provides the temporary Linux runner;
- the SDK is officially published by Google;
- `GEMINI_API_KEY` can be supplied as a GitHub Actions secret;
- the agent can inspect and modify the checked-out repository;
- the result can be committed back to GitHub.

## API key

There is not a separate secret called an "Antigravity API key" that we need to invent. For the SDK path, the credential is a **Gemini API key** exposed as:

`GEMINI_API_KEY`

Create the key in Google AI Studio, then put it in GitHub:

`AI-AGENT → Settings → Secrets and variables → Actions → New repository secret`

Name: `GEMINI_API_KEY`

Value: your Gemini API key.

The key must never be committed to the repository.

## Future cloud mode

For the next stage, AI-AGENT can add a `managed` execution backend:

`GitHub task → Managed Antigravity Agent API → remote environment → result → GitHub`

That path is preferable if we want the executor to be independent of GitHub's ephemeral runner and to scale beyond one workflow at a time.
