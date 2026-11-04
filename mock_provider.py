#!/usr/bin/env python3
"""Small local external-provider mock for testing the transfer application."""

import json
import os
from http.server import BaseHTTPRequestHandler, HTTPServer

PORT = int(os.getenv("MOCK_PROVIDER_PORT", "8081"))
TRANSFERS = {}


class ProviderHandler(BaseHTTPRequestHandler):
    def _send(self, status, payload):
        body = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        prefix = "/transfers/by-idempotency-key/"
        if self.path.startswith(prefix):
            key = self.path[len(prefix):]
            result = TRANSFERS.get(key)
            if result:
                self._send(200, result)
            else:
                self._send(404, {"message": "transfer not found"})
            return
        self._send(404, {"message": "not found"})

    def do_POST(self):
        if self.path != "/transfers":
            self._send(404, {"message": "not found"})
            return

        key = self.headers.get("Idempotency-Key")
        if not key:
            self._send(400, {"message": "Idempotency-Key header is required"})
            return

        # Duplicate submissions return the original provider result.
        if key in TRANSFERS:
            self._send(200, TRANSFERS[key])
            return

        length = int(self.headers.get("Content-Length", "0"))
        request = json.loads(self.rfile.read(length) or b"{}")
        result = {
            "providerTransferId": "mock-" + key,
            "message": "accepted by local mock provider",
        }
        TRANSFERS[key] = result
        print(f"accepted clientReference={request.get('clientReference')} key={key}", flush=True)
        self._send(201, result)

    def log_message(self, format, *args):
        return


if __name__ == "__main__":
    print(f"Mock provider listening on http://localhost:{PORT}", flush=True)
    HTTPServer(("0.0.0.0", PORT), ProviderHandler).serve_forever()

