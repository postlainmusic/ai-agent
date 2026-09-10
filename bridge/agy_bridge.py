#!/usr/bin/env python3
"""Small localhost bridge between the Android shell and the Antigravity CLI.

Run inside the same Linux/Termux environment where `agy` is installed.
The Android app can POST JSON to /run with {"prompt": "...", "project": "/path"}.
"""

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
import subprocess

HOST = "127.0.0.1"
PORT = 8765


class Handler(BaseHTTPRequestHandler):
    def _json(self, status, payload):
        body = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/health":
            self._json(200, {"ok": True, "engine": "agy"})
            return
        self._json(404, {"error": "not_found"})

    def do_POST(self):
        if self.path != "/run":
            self._json(404, {"error": "not_found"})
            return

        length = int(self.headers.get("Content-Length", "0"))
        try:
            data = json.loads(self.rfile.read(length) or b"{}")
            prompt = str(data.get("prompt", "")).strip()
            project = os.path.abspath(str(data.get("project", os.getcwd())))
            if not prompt:
                raise ValueError("prompt is required")

            result = subprocess.run(
                [
                    "agy",
                    "--dangerously-skip-permissions",
                    "--output-format",
                    "json",
                    "--project",
                    project,
                    "-p",
                    prompt,
                ],
                cwd=project,
                text=True,
                capture_output=True,
                timeout=900,
            )
            self._json(200, {
                "ok": result.returncode == 0,
                "exitCode": result.returncode,
                "stdout": result.stdout,
                "stderr": result.stderr,
            })
        except Exception as exc:
            self._json(400, {"ok": False, "error": str(exc)})


if __name__ == "__main__":
    print(f"Antigravity bridge listening on http://{HOST}:{PORT}")
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
