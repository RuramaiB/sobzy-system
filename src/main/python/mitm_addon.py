"""
mitmproxy addon for Intelligent Web Access Control System
Intersects traffic and communicates with the Spring Boot backend
for real-time classification and enforcement.
Now includes DNS Hijacking for Zero-Config Captive Portal.
"""

import json
import requests
import socket
import threading
from mitmproxy import http
from dnslib import DNSRecord, QTYPE, RR, A

class WebAccessControl:
    def __init__(self):
        # Dynamically detect Host IP (Hotspot Gateway)
        self.host_ip = self._get_host_ip()
        print(f"[*] IWACS Addon Started. Host IP detected as: {self.host_ip}")

        self.portal_url = f"http://{self.host_ip}:3000/login"
        self.auth_check_url = f"http://{self.host_ip}:8080/api/v1/portal/check-auth"
        self.classify_url = f"http://{self.host_ip}:8080/api/v1/traffic/classify"
        
        # OS Captive Portal Trigger URLs
        self.ncsi_urls = [
            "msftconnecttest.com",
            "msftncsi.com",
            "clients3.google.com/generate_204",
            "connectivitycheck.gstatic.com",
            "connectivitycheck.android.com",
            "clients1.google.com/generate_204",
            "captive.apple.com",
            "appleid.apple.com",
            "thinkdifferent.us"
        ]

        # Start DNS Hijacking Server
        self._start_dns_server()

    def _get_host_ip(self):
        """Robustly detect the hotspot bridge IP on Windows"""
        import subprocess
        print("[*] Detecting Hotspot IP...")
        try:
            # Query PowerShell for adapters with 172.24 or 192.168.137 IPs (Common Windows Hotspot ranges)
            cmd = "Get-NetIPAddress | Where-Object { $_.AddressFamily -eq 'IPv4' -and ($_.IPAddress -like '172.24.*' -or $_.IPAddress -like '192.168.137.*') } | Select-Object -ExpandProperty IPAddress"
            proc = subprocess.run(["powershell", "-Command", cmd], capture_output=True, text=True)
            ips = proc.stdout.strip().split()
            if ips:
                print(f"[*] Detected Hotspot IP from PowerShell: {ips[0]}")
                return ips[0]
        except Exception as e:
            print(f"[!] PowerShell IP detection failed: {e}")
        
        # Fallback to current method
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            print(f"[*] Detected IP via 8.8.8.8: {ip}")
            return ip
        except Exception:
            print("[!] All IP detection failed, falling back to 192.168.137.1")
            return "192.168.137.1"

    def _start_dns_server(self):
        """Start a DNS server thread to hijack unauthenticated traffic"""
        def run_dns():
            print(f"[*] Starting DNS Hijacker on {self.host_ip}:53")
            udp_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            try:
                udp_sock.bind((self.host_ip, 53))
            except Exception as e:
                print(f"[!] DNS Server failed to bind to {self.host_ip}:53 - {e}")
                return

            while True:
                data, addr = udp_sock.recvfrom(1024)
                client_ip = addr[0]
                try:
                    request = DNSRecord.parse(data)
                    qname = str(request.q.qname).rstrip('.')
                    
                    # Check Authentication Status
                    is_auth = False
                    try:
                        resp = requests.get(self.auth_check_url, headers={"X-Forwarded-For": client_ip}, timeout=0.5)
                        if resp.status_code == 200:
                            is_auth = resp.json().get("authenticated", False)
                    except: pass

                    reply = request.reply()
                    
                    # Hijack Logic
                    if not is_auth and qname not in ["localhost"]:
                        # Point everything to the Portal machine
                        reply.add_answer(RR(qname, QTYPE.A, rdata=A(self.host_ip), ttl=60))
                        print(f"[*] DNS HIJACK: {client_ip} queried {qname} -> {self.host_ip}")
                    else:
                        # Forward to real DNS (8.8.8.8) for authenticated users or portal checks
                        try:
                            real_dns_resp = request.send("8.8.8.8", 53, timeout=1.0)
                            reply = DNSRecord.parse(real_dns_resp)
                        except Exception as e:
                            print(f"[!] DNS Forward failed for {qname}: {e}")
                    
                    udp_sock.sendto(reply.pack(), addr)
                except Exception as e:
                    print(f"[!] DNS Processing error: {e}")

        dns_thread = threading.Thread(target=run_dns, daemon=True)
        dns_thread.start()

    def request(self, flow: http.HTTPFlow) -> None:
        url = flow.request.pretty_url
        client_ip = flow.client_conn.peername[0]
        
        # 1. Skip checks for infrastructure
        if self.host_ip in url or "localhost" in url or "127.0.0.1" in url:
            return

        # 2. Captive Portal Detection Trigger (NCSI)
        is_ncsi = any(ncsi in url for ncsi in self.ncsi_urls)
        
        # 3. Check for Capture Portal Authentication via IP
        try:
            resp = requests.get(self.auth_check_url, headers={"X-Forwarded-For": client_ip}, timeout=1.0)
            if resp.status_code == 200:
                auth_data = resp.json()
                if not auth_data.get("authenticated", False):
                    # Trigger Zero-Config Popup
                    if is_ncsi or flow.request.port == 80:
                        print(f"[*] FORCING Portal Popup for {client_ip} searching {url}")
                        flow.response = http.Response.make(
                            200,
                            f"<html><head><meta http-equiv='refresh' content='0;url={self.portal_url}'></head>"
                            f"<body><h1>Login Required</h1><p>Wait while we redirect you to the <a href='{self.portal_url}'>Hotspot Login</a>.</p></body></html>".encode(),
                            {"Content-Type": "text/html"}
                        )
                    else:
                        # Standard 302 for non-NCSI HTTP
                        flow.response = http.Response.make(
                            302,
                            b"",
                            {"Location": self.portal_url}
                        )
                    return
        except Exception as e:
            print(f"Auth check failed: {e}")
            pass

        # 4. Traffic Classification & Extraction
        try:
            payload = {
                "url": url,
                "method": flow.request.method,
                "ipAddress": client_ip,
                "userAgent": flow.request.headers.get("User-Agent", "Unknown"),
                "referer": flow.request.headers.get("Referer", ""),
                "deviceId": "hotspot-device"
            }
            requests.post(self.classify_url, json=payload, timeout=2.0)
        except Exception as e:
            print(f"Classification logging failed: {e}")

addons = [
    WebAccessControl()
]
