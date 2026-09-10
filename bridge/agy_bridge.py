#!/usr/bin/env python3
"""Local Antigravity bridge: filesystem + terminal + agy agent."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json, os, subprocess

HOST, PORT = "127.0.0.1", 8765
MAX_OUTPUT = 120_000


def run(cmd, cwd=None, timeout=30):
    p = subprocess.run(cmd, cwd=cwd, text=True, capture_output=True, timeout=timeout)
    return {"ok": p.returncode == 0, "exitCode": p.returncode,
            "stdout": p.stdout[-MAX_OUTPUT:], "stderr": p.stderr[-MAX_OUTPUT:]}


class Handler(BaseHTTPRequestHandler):
    def _json(self, status, payload):
        body = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers(); self.wfile.write(body)

    def do_OPTIONS(self): self._json(204, {})

    def do_GET(self):
        if self.path == "/health":
            self._json(200, {"ok": True, "engine": "agy", "cwd": os.getcwd()})
        elif self.path == "/files":
            try:
                items = []
                for name in sorted(os.listdir(os.getcwd())):
                    if name.startswith('.') and name not in ('.github',): continue
                    path = os.path.join(os.getcwd(), name)
                    items.append({"name": name, "directory": os.path.isdir(path)})
                self._json(200, {"ok": True, "items": items})
            except Exception as e: self._json(400, {"ok": False, "error": str(e)})
        else: self._json(404, {"error": "not_found"})

    def do_POST(self):
        if self.path not in ("/run", "/terminal"):
            self._json(404, {"error": "not_found"}); return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            data = json.loads(self.rfile.read(length) or b"{}")
            cwd = os.path.abspath(str(data.get("project", os.getcwd())))
            if not os.path.isdir(cwd): raise ValueError("project directory does not exist")
            if self.path == "/terminal":
                command = str(data.get("command", "")).strip()
                if not command: raise ValueError("command is required")
                self._json(200, run(["bash", "-lc", command], cwd, 120)); return
            prompt = str(data.get("prompt", "")).strip()
            if not prompt: raise ValueError("prompt is required")
            result = run(["agy", "--dangerously-skip-permissions", "--output-format", "json", "--project", cwd, "-p", prompt], cwd, 900)
            self._json(200, result)
        except subprocess.TimeoutExpired:
            self._json(408, {"ok": False, "error": "process timed out"})
        except Exception as exc: self._json(400, {"ok": False, "error": str(exc)})


if __name__ == "__main__":
    print(f"Antigravity bridge listening on http://{HOST}:{PORT}")
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
