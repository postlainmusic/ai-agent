import asyncio
import json
import os
from pathlib import Path
from typing import Any

from google.antigravity import Agent, LocalAgentConfig

ROOT = Path(__file__).resolve().parent
INBOX = ROOT / "tasks" / "inbox"
RESULTS = ROOT / "tasks" / "results"


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


async def run() -> None:
    if not os.environ.get("GEMINI_API_KEY"):
        raise RuntimeError("GEMINI_API_KEY is not configured")

    task = load_task()
    prompt = build_prompt(task)

    config = LocalAgentConfig(
        system_instructions=(
            "You are a careful software execution agent. "
            "Follow the task exactly, minimize unnecessary changes, "
            "and never print secrets."
        )
    )

    async with Agent(config) as agent:
        response = await agent.chat(prompt)
        text = await response.text()

    result = {
        "task_id": task["id"],
        "status": "completed",
        "summary": text,
        "changes": [],
        "tests": [],
        "error": None,
    }

    RESULTS.mkdir(parents=True, exist_ok=True)
    output = RESULTS / f"{task['id']}.json"
    with output.open("w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
        f.write("\n")


if __name__ == "__main__":
    asyncio.run(run())
