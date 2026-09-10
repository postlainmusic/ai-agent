import json
import os
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent
INBOX = ROOT / "tasks" / "inbox"
RESULTS = ROOT / "tasks" / "results"
OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"


def load_task() -> dict[str, Any]:
    files = sorted(INBOX.glob("*.json"))
    if not files:
        raise RuntimeError("No task found in tasks/inbox")
    with files[0].open("r", encoding="utf-8") as f:
        return json.load(f)


def build_prompt(task: dict[str, Any]) -> str:
    criteria = "\n".join(f"- {x}" for x in task.get("success_criteria", []))
    constraints = "\n".join(f"- {x}" for x in task.get("constraints", [])) or "- None"
    context = task.get("context", "")
    return f"""You are the execution agent for the AI-AGENT system.

TASK ID: {task['id']}

GOAL:
{task['goal']}

CONTEXT:
{context}

SUCCESS CRITERIA:
{criteria}

CONSTRAINTS:
{constraints}

Work directly in the current repository when changes are required.
Inspect before editing. Run appropriate tests after changes.
Do not expose secrets, tokens, or credentials in output.
At the end, report:
1. status: completed, failed, blocked, or needs_review
2. summary
3. files changed
4. tests/checks performed
5. remaining risks or blockers
"""


def call_openrouter(prompt: str) -> str:
    api_key = os.environ.get("OPENROUTER_API_KEY")
    if not api_key:
        raise RuntimeError("OPENROUTER_API_KEY is not configured")

    payload = {
        "model": "openrouter/free",
        "messages": [
            {
                "role": "system",
                "content": (
                    "You are a careful software execution agent. "
                    "Follow the task exactly, minimize unnecessary changes, "
                    "and never print secrets."
                ),
            },
            {"role": "user", "content": prompt},
        ],
    }

    request = urllib.request.Request(
        OPENROUTER_URL,
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
            "HTTP-Referer": "https://github.com/postlainmusic/AI-AGENT",
            "X-Title": "AI-AGENT",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            data = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"OpenRouter HTTP {exc.code}: {body[:500]}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"OpenRouter connection error: {exc.reason}") from exc

    try:
        return data["choices"][0]["message"]["content"]
    except (KeyError, IndexError, TypeError) as exc:
        raise RuntimeError(f"Unexpected OpenRouter response: {json.dumps(data)[:500]}") from exc


def write_result(task: dict[str, Any], status: str, summary: str, error: str | None = None) -> None:
    result = {
        "task_id": task["id"],
        "status": status,
        "summary": summary,
        "changes": [],
        "tests": [],
        "error": error,
    }
    RESULTS.mkdir(parents=True, exist_ok=True)
    output = RESULTS / f"{task['id']}.json"
    with output.open("w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
        f.write("\n")


def run() -> None:
    task = load_task()
    prompt = build_prompt(task)
    try:
        text = call_openrouter(prompt)
        write_result(task, "completed", text)
    except Exception as exc:
        write_result(task, "failed", "OpenRouter execution failed.", str(exc))
        raise


if __name__ == "__main__":
    run()
