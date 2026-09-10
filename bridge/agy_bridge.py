#!/usr/bin/env python3
"""Local Antigravity bridge: workspace filesystem, terminal, builds and agy."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs
import json, os, subprocess

HOST, PORT = "127.0.0.1", 8765
ROOT = os.path.realpath(os.environ.get("AGY_PROJECT", os.getcwd()))
MAX_OUTPUT = 120_000
MAX_FILE = 2_000_000


def safe_path(rel=""):
    rel = str(rel or "").replace("\\", "/")
    candidate = os.path.realpath(os.path.join(ROOT, rel.lstrip("/")))
    if candidate != ROOT and not candidate.startswith(ROOT + os.sep):
        raise ValueError("path escapes project root")
    return candidate


def run(cmd, cwd=None, timeout=30):
    p = subprocess.run(cmd, cwd=cwd, text=True, capture_output=True, timeout=timeout)
    return {"ok": p.returncode == 0, "exitCode": p.returncode,
            "stdout": p.stdout[-MAX_OUTPUT:], "stderr": p.stderr[-MAX_OUTPUT:]}


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        return

    def _json(self, status, payload):
        body = json.dumps(payload, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _body(self):
        length = int(self.headers.get("Content-Length", "0"))
        return json.loads(self.rfile.read(length) or b"{}")

    def do_OPTIONS(self): self._json(204, {})

    def do_GET(self):
        try:
            parsed = urlparse(self.path)
            q = parse_qs(parsed.query)
            if parsed.path == "/health":
                self._json(200, {"ok": True, "engine": "agy", "cwd": ROOT, "agy": os.path.realpath(os.environ.get("HOME", ""))})
                return
            if parsed.path == "/files":
                directory = safe_path(q.get("path", [""])[0])
                items = []
                for name in sorted(os.listdir(directory), key=lambda x: (not os.path.isdir(os.path.join(directory, x)), x.lower())):
                    if name.startswith(".") and name not in (".github",): continue
                    p = os.path.join(directory, name)
                    items.append({"name": name, "path": os.path.relpath(p, ROOT), "directory": os.path.isdir(p)})
                self._json(200, {"ok": True, "path": os.path.relpath(directory, ROOT), "items": items})
                return
            if parsed.path == "/read":
                p = safe_path(q.get("path", [""])[0])
                if not os.path.isfile(p): raise ValueError("file not found")
                if os.path.getsize(p) > MAX_FILE: raise ValueError("file is too large")
                with open(p, "r", encoding="utf-8", errors="replace") as f: content = f.read()
                self._json(200, {"ok": True, "path": os.path.relpath(p, ROOT), "content": content})
                return
            self._json(404, {"error": "not_found"})
        except Exception as e:
            self._json(400, {"ok": False, "error": str(e)})

    def do_POST(self):
        try:
            parsed = urlparse(self.path)
            data = self._body()
            cwd = safe_path(data.get("project", ""))
            if not os.path.isdir(cwd): raise ValueError("project directory does not exist")

            if parsed.path == "/write":
                rel = data.get("path", "")
                content = str(data.get("content", ""))
                if len(content.encode("utf-8")) > MAX_FILE: raise ValueError("file is too large")
                p = safe_path(rel)
                os.makedirs(os.path.dirname(p), exist_ok=True)
                with open(p, "w", encoding="utf-8", newline="") as f: f.write(content)
                self._json(200, {"ok": True, "path": os.path.relpath(p, ROOT)})
                return

            if parsed.path == "/terminal":
                command = str(data.get("command", "")).strip()
                if not command: raise ValueError("command is required")
                self._json(200, run(["bash", "-lc", command], cwd, 120)); return

            if parsed.path == "/build":
                command = str(data.get("command", "./gradlew assembleDebug"))
                self._json(200, run(["bash", "-lc", command], cwd, 900)); return

            if parsed.path == "/run":
                prompt = str(data.get("prompt", "")).strip()
                if not prompt: raise ValueError("prompt is required")
                result = run(["agy", "--dangerously-skip-permissions", "--output-format", "json", "--project", cwd, "-p", prompt], cwd, 900)
                self._json(200, result); return

            self._json(404, {"error": "not_found"})
        except subprocess.TimeoutExpired:
            self._json(408, {"ok": False, "error": "process timed out"})
        except Exception as exc:
            self._json(400, {"ok": False, "error": str(exc)})


if __name__ == "__main__":
    if not os.path.isdir(ROOT): raise SystemExit(f"Project does not exist: {ROOT}")
    print(f"Antigravity bridge listening on http://{HOST}:{PORT}")
    print(f"Project root: {ROOT}")
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
