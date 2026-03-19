import json
import logging
import requests
import threading
import time
from datetime import datetime
from mitmproxy import http

# Configuration
BACKEND_URL = "http://localhost:1998/api/traffic/ingest"
POLL_INTERVAL = 30 # Seconds to fallback poll if no traffic

class TrafficIngestor:
    def __init__(self):
        self.deny_list = set()
        self.lock = threading.Lock()
        self.last_sync = 0
        logging.info("Mitmproxy Traffic Ingestor Addon Initialized")
        # Initial sync
        self.sync_deny_list()

    def sync_deny_list(self, new_list=None):
        if new_list is not None:
            with self.lock:
                self.deny_list = set(new_list)
            self.last_sync = time.time()
            logging.info(f"Deny list updated. Total domains: {len(self.deny_list)}")
            return

        try:
            response = requests.get("http://localhost:8080/api/traffic/blocked-domains", timeout=5)
            if response.status_code == 200:
                with self.lock:
                    self.deny_list = set(response.json())
                self.last_sync = time.time()
                logging.info(f"Deny list synced from backend. Total domains: {len(self.deny_list)}")
        except Exception as e:
            logging.error(f"Failed to sync deny list: {e}")

    def request(self, flow: http.HTTPFlow) -> None:
        # Check deny list
        host = flow.request.pretty_host
        with self.lock:
            if host in self.deny_list:
                logging.info(f"Blocking request to {host}")
                flow.response = http.Response.make(
                    403,
                    b"Blocked by Sobzy Security Policy",
                    {"Content-Type": "text/plain"}
                )
                return

    def response(self, flow: http.HTTPFlow) -> None:
        # Prepare payload
        payload = {
            "clientIp": flow.client_conn.peername[0],
            "host": flow.request.pretty_host,
            "url": flow.request.url,
            "method": flow.request.method,
            "requestHeaders": dict(flow.request.headers),
            "requestBody": flow.request.get_text(limit=10000) if flow.request.content else "",
            "responseCode": flow.response.status_code,
            "responseHeaders": dict(flow.response.headers),
            "responseBody": flow.response.get_text(limit=20000) if flow.response.content else "",
            "timestamp": datetime.now().isoformat()
        }

        # Send to Spring Boot asynchronously
        threading.Thread(target=self.send_to_backend, args=(payload,)).start()

    def send_to_backend(self, payload):
        try:
            response = requests.post(BACKEND_URL, json=payload, timeout=10)
            if response.status_code == 200:
                # Backend returns updated deny list
                self.sync_deny_list(response.json())
        except Exception as e:
            logging.error(f"Error sending traffic to backend: {e}")

addons = [
    TrafficIngestor()
]
