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
import sys
from mitmproxy import http
from dnslib import DNSRecord, QTYPE, RR, A

class WebAccessControl:
    def __init__(self):
        # Dynamically detect Host IP (Hotspot Gateway)
        self.host_ip = self._get_host_ip()
        print(f"[*] IWACS Addon Started. Host IP detected as: {self.host_ip}")

        self.portal_url = f"http://{self.host_ip}:3000/login"
        self.auth_check_url = f"http://{self.host_ip}:1998/api/v1/portal/check-auth"
        self.classify_url = f"http://{self.host_ip}:1998/api/v1/traffic/classify"
        
        print(f"[*] Backend URLs configured: Auth={self.auth_check_url}, Classify={self.classify_url}")
        
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
                    
                    # WPAD Support for Auto-Proxy Discovery
                    if qname.lower() in ["wpad", "wpad.local", "wpad.home"]:
                        reply = request.reply()
                        reply.add_answer(RR(qname, QTYPE.A, rdata=A(self.host_ip), ttl=60))
                        udp_sock.sendto(reply.pack(), addr)
                        print(f"[*] DNS WPAD RESOLVE: {client_ip} -> {self.host_ip}")
                        sys.stdout.flush()
                        continue

                    # Check Authentication Status
                    is_auth = False
                    try:
                        resp = requests.get(self.auth_check_url, headers={"X-Forwarded-For": client_ip}, timeout=0.5)
                        if resp.status_code == 200:
                            is_auth = resp.json().get("authenticated", False)
                    except: pass

                    reply = request.reply()
                    
                    # Hijack Logic
                    if not is_auth and qname not in ["localhost", self.host_ip]:
                        # Point everything to the Portal machine
                        reply.add_answer(RR(qname, QTYPE.A, rdata=A(self.host_ip), ttl=60))
                        # print(f"[*] DNS HIJACK: {client_ip} queried {qname} -> {self.host_ip}")
                    else:
                        # Forward to real DNS (8.8.8.8)
                        # print(f"[*] DNS FORWARD: {qname}")
                        try:
                            # Avoid forwarding wpad if not authenticated yet to real dns
                            real_dns_resp = request.send("8.8.8.8", 53, timeout=1.0)
                            reply = DNSRecord.parse(real_dns_resp)
                        except Exception as e:
                            print(f"[!] DNS Forward failed for {qname}: {e}")
                    
                    udp_sock.sendto(reply.pack(), addr)
                except Exception as e:
                    # print(f"[!] DNS Processing error: {e}")
                    pass

    def request(self, flow: http.HTTPFlow) -> None:
        url = flow.request.pretty_url
        client_ip = flow.client_conn.peername[0]
        
        # 0. Fix for transparently redirected packets on Windows
        if not flow.request.host or flow.request.scheme == "":
            host_header = flow.request.headers.get("Host", "")
            if host_header:
                flow.request.host = host_header
                flow.request.scheme = "https" if flow.request.port == 443 else "http"
                url = flow.request.pretty_url
                print(f"[*] Rectified transparent request: {url}")
                sys.stdout.flush()
        # 1. WPAD / PAC File Serving (Port 80 hijacking)
        if "/wpad.dat" in flow.request.path or "proxy.pac" in flow.request.path:
            proxy_port = 8080 # Default mitmproxy port
            pac_content = f"""
            function FindProxyForURL(url, host) {{
                if (isPlainHostName(host) || shExpMatch(host, "*.local") || host == "{self.host_ip}")
                    return "DIRECT";
                return "PROXY {self.host_ip}:{proxy_port}; DIRECT";
            }}
            """
            flow.response = http.Response.make(
                200, 
                pac_content.encode(), 
                {"Content-Type": "application/x-ns-proxy-autoconfig", "Access-Control-Allow-Origin": "*"}
            )
            print(f"[*] Served WPAD.DAT to {client_ip}")
            return

        # 1.5 LOG EVERY REQUEST FOR DEBUGGING
        print(f"[DEBUG] Intercepted: {client_ip} -> {url}")

        # 2. Specialized Proxy Routes (Admin/Logs/Certs)
        if "/____iwacs_log" in url:
            try:
                log_data = json.loads(flow.request.content)
                log_data["ipAddress"] = client_ip
                event_api = f"http://{self.host_ip}:1998/api/v1/traffic/events"
                requests.post(event_api, json=log_data, timeout=1.0)
                flow.response = http.Response.make(204, b"", {"Access-Control-Allow-Origin": "*"})
                return
            except Exception as e:
                print(f"[!] Log interception failed: {e}")
                sys.stdout.flush()
                flow.response = http.Response.make(500, b"Log error")
                return

        if "/____iwacs_cert" in url:
            try:
                import os
                cert_path = os.path.expanduser("~/.mitmproxy/mitmproxy-ca-cert.cer")
                if os.path.exists(cert_path):
                    with open(cert_path, "rb") as f:
                        flow.response = http.Response.make(200, f.read(), {"Content-Type": "application/x-x509-ca-cert"})
                    return
            except: pass

        # 3. Skip infrastructure
        if self.host_ip in url or "localhost" in url:
            return

        # 4. Traffic Classification (Log Everything)
        is_allowed = True
        try:
            payload = {
                "url": url,
                "method": flow.request.method,
                "ipAddress": client_ip,
                "userAgent": flow.request.headers.get("User-Agent", "Unknown"),
                "referer": flow.request.headers.get("Referer", "")
            }
            resp = requests.post(self.classify_url, json=payload, timeout=2.0)
            if resp.status_code == 200:
                result = resp.json()
                if result.get("decision") == "BLOCK" or not result.get("is_allowed", True):
                    is_allowed = False
        except Exception as e: 
            print(f"[!] Traffic Classification loop error: {e}")
            pass

        if not is_allowed:
            flow.response = http.Response.make(403, b"Blocked by IWACS", {"Content-Type": "text/html"})
            return

        # 5. Captive Portal Redirect for Unauthenticated
        is_ncsi = any(ncsi in url for ncsi in self.ncsi_urls)
        try:
            resp = requests.get(self.auth_check_url, headers={"X-Forwarded-For": client_ip}, timeout=1.0)
            if resp.status_code == 200 and not resp.json().get("authenticated", False):
                if is_ncsi or flow.request.port == 80:
                    portal_html = f"<html><head><meta http-equiv='refresh' content='0;url={self.portal_url}'></head><body>Redirecting to <a href='{self.portal_url}'>Login</a></body></html>"
                    flow.response = http.Response.make(200, portal_html.encode(), {"Content-Type": "text/html"})
                    return
                else:
                    flow.response = http.Response.make(302, b"", {"Location": self.portal_url})
                    return
        except: pass

    def response(self, flow: http.HTTPFlow) -> None:
        """Inject browser activity tracker"""
        if flow.response and "text/html" in flow.response.headers.get("Content-Type", ""):
            if flow.response.status_code == 200 and flow.response.content:
                try:
                    tracker_script = f"""
                    <script id="iwacs-tracker">
                    (function() {{
                        const apiEndpoint = "/____iwacs_log";
                        function sendEvent(type, details) {{
                            fetch(apiEndpoint, {{
                                method: 'POST',
                                headers: {{ 'Content-Type': 'text/plain' }}, 
                                body: JSON.stringify({{ eventType: type, url: window.location.href, details: details, timestamp: new Date().toISOString() }})
                            }}).catch(() => {{}});
                        }}
                        document.addEventListener('click', (e) => {{
                            const t = e.target.closest('a, button, input[type="submit"]');
                            if (t) sendEvent('CLICK', (t.innerText || t.value || t.tagName).substring(0, 50).trim());
                        }}, true);
                        setInterval(() => {{ /* Navigation / Title tracking */ }}, 5000);
                    }})();
                    </script>
                    """
                    import re
                    content = flow.response.content.decode("utf-8", "ignore")
                    if re.search(r'</body', content, re.I):
                        content = re.sub(r'(</body)', tracker_script + r'\1', content, 1, re.I)
                    elif re.search(r'</html', content, re.I):
                        content = re.sub(r'(</html)', tracker_script + r'\1', content, 1, re.I)
                    else:
                        content += tracker_script
                    flow.response.content = content.encode("utf-8")
                except: pass

addons = [
    WebAccessControl()
]
