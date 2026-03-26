package com.example.sobzybackend.service;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.filters.RequestFilter;
import com.browserup.bup.filters.ResponseFilter;
import com.browserup.bup.util.HttpMessageContents;
import com.browserup.bup.util.HttpMessageInfo;
import com.example.sobzybackend.service.ClassificationService;
import com.example.sobzybackend.service.TrafficLogService;

import com.example.sobzybackend.dtos.ClassificationRequest;
import com.example.sobzybackend.dtos.ClassificationResult;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@Service
public class EmbeddedProxyServer {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedProxyServer.class);

    private final PortalService portalService;
    private final TrafficLogService trafficLogService;
    private final ClassificationService classificationService;

    public EmbeddedProxyServer(PortalService portalService,
                               TrafficLogService trafficLogService,
                               ClassificationService classificationService) {
        this.portalService = portalService;
        this.trafficLogService = trafficLogService;
        this.classificationService = classificationService;
    }

    private BrowserUpProxy proxy;
    private boolean isRunning = false;

    private static final String BLOCK_PAGE_HTML = "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>Access Restricted | IWACS</title><link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;600;800&display=swap' rel='stylesheet'><style>:root{--bg:#0a0a0a;--card-bg:#141414;--text-primary:#fff;--text-secondary:#a1a1aa;--accent:#ef4444;--border:#27272a}*{margin:0;padding:0;box-sizing:border-box}body{background-color:var(--bg);color:var(--text-primary);font-family:Inter,-apple-system,sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;overflow:hidden}.container{max-width:500px;width:90%;text-align:center;background:var(--card-bg);padding:3rem 2rem;border-radius:24px;border:1px solid var(--border);box-shadow:0 25px 50px -12px rgba(0,0,0,.5);animation:fadeIn .6s cubic-bezier(.16,1,.3,1)}@keyframes fadeIn{from{opacity:0;transform:translateY(20px)}to{opacity:1;transform:translateY(0)}}.icon-box{background:rgba(239,68,68,.1);width:80px;height:80px;border-radius:20px;display:flex;align-items:center;justify-content:center;margin:0 auto 1.5rem;color:var(--accent)}h1{font-size:1.875rem;font-weight:800;margin-bottom:.75rem;letter-spacing:-.025em}p{color:var(--text-secondary);line-height:1.6;margin-bottom:2rem}.details{background:rgba(255,255,255,.03);border:1px solid var(--border);border-radius:12px;padding:1rem;margin-bottom:2rem;text-align:left}.detail-row{display:flex;justify-content:space-between;margin-bottom:.5rem;font-size:.875rem}.detail-row:last-child{margin-bottom:0}.label{color:var(--text-secondary)}.value{color:var(--text-primary);font-weight:600;word-break:break-all}.btn{display:inline-block;background:var(--text-primary);color:var(--bg);padding:.75rem 2rem;border-radius:12px;font-weight:600;text-decoration:none;transition:transform .2s,background .2s}.btn:hover{background:#e4e4e7;transform:translateY(-2px)}.footer{margin-top:2rem;font-size:.75rem;color:#52525b}</style></head><body><div class='container'><div class='icon-box'><svg xmlns='http://www.w3.org/2000/svg' width='40' height='40' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'><path d='M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z'></path><line x1='12' y1='8' x2='12' y2='12'></line><line x1='12' y1='16' x2='12.01' y2='16'></line></svg></div><h1>Access Restricted</h1><p>Your request to access this website has been blocked due to network security policies.</p><div class='details'><div class='detail-row'><span class='label'>Website</span><span class='value'>{{DOMAIN}}</span></div><div class='detail-row'><span class='label'>Reason</span><span class='value'>{{REASON}}</span></div></div><a href='javascript:history.back()' class='btn'>Go Back</a><div class='footer'>Powered by IWACS Intelligence Systems</div></div></body></html>";

    public void startProxy(String hostIp) {
        if (isRunning)
            return;

        try {
            proxy = new BrowserUpProxyServer();
            proxy.setTrustAllServers(true);

            // MITM is DISABLED intentionally.
            // For a captive portal, we only need to intercept HTTP (port 80) to redirect unauthenticated users.
            // HTTPS is tunneled transparently — no cert errors, no certificate_unknown rejections.
            // Attempting MITM causes NullPointerException in LittleProxy when bypassing per-connection.
            proxy.setMitmDisabled(true);

            // Listen on port 8080
            proxy.start(8080);

            // Inject Request and Response filters
            configureFilters(hostIp);

            isRunning = true;
            log.info("Embedded Transparent Proxy started successfully on {}:8080", hostIp);
        } catch (Exception e) {
            log.error("Failed to start Embedded MITM Proxy", e);
            isRunning = false;
        }
    }

    public void stopProxy() {
        if (proxy != null && isRunning) {
            proxy.abort();
            isRunning = false;
            log.info("Embedded MITM Proxy stopped.");
        }
    }

    private void configureFilters(String hostIp) {
        String portalUrl = "http://" + hostIp + ":3000/login";
        String pacContent = String.format(
                "function FindProxyForURL(url, host) { if (isPlainHostName(host) || shExpMatch(host, \"*.local\") || host == \"%s\") return \"DIRECT\"; return \"PROXY %s:8080; DIRECT\"; }",
                hostIp, hostIp);

        proxy.addRequestFilter(new RequestFilter() {
            @Override
            public HttpResponse filterRequest(HttpRequest request, HttpMessageContents contents,
                    HttpMessageInfo messageInfo) {
                String url = messageInfo.getOriginalUrl();
                InetSocketAddress socketAddress = (InetSocketAddress) messageInfo.getChannelHandlerContext().channel()
                        .remoteAddress();
                String clientIp = socketAddress.getAddress().getHostAddress();
                // Add X-Forwarded-For header so backend knows the real client IP
                request.headers().set("X-Forwarded-For", clientIp);

                if (url.contains("/wpad.dat") || url.contains("proxy.pac")) {
                    HttpResponse pacResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                    ((DefaultFullHttpResponse) pacResponse).content()
                            .writeBytes(pacContent.getBytes(StandardCharsets.UTF_8));
                    pacResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-ns-proxy-autoconfig");
                    return pacResponse;
                }

                String hostHeader = request.headers().get(HttpHeaderNames.HOST);
                boolean isAuth = portalService.isIpAuthenticated(clientIp);

                // 1. Local/Portal Traffic Bypass & Redirection
                if (url.contains(hostIp) || url.contains("localhost")
                        || (hostHeader != null && (hostHeader.contains(hostIp) || hostHeader.contains(":1998")
                                || hostHeader.contains(":3000")))) {
                    
                    // IF it's a request to Host IP on port 80 (likely redirected from 80), 302 to Portal
                    // This prevents the proxy-to-host loop
                    if (hostHeader != null && (hostHeader.equals(hostIp) || hostHeader.equals(hostIp + ":80"))) {
                         log.info("LOCAL_LOOP_PREVENT: Redirecting direct host request from {} to portal", clientIp);
                         HttpResponse redirect = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
                         redirect.headers().set(HttpHeaderNames.LOCATION, portalUrl);
                         return redirect;
                    }

                    log.debug("Proxy Bypassed for portal/local traffic: URI={}, Host={}", url, hostHeader);
                    return null;
                }

                // 2. Captive Portal Probe + Auth Check — unauthenticated users ALWAYS get redirected
                // Whitelist bypass MUST NOT apply here for unauthenticated users.
                // OS captive portal probes use whitelisted domains (gstatic.com, apple.com, msftconnecttest.com)
                // so we must intercept them to show the "Sign in to network" notification.
                boolean isWhitelisted = classificationService.isWhitelisted(extractDomainFromUrl(url));

                if (!isAuth) {
                    if (request.method().equals(HttpMethod.CONNECT)) {
                        // HTTPS CONNECT from unauthenticated: deny with 403.
                        // This is what triggers Android/iOS/Windows to detect a captive portal
                        // (the OS falls back to HTTP check → gets 302 → shows portal notification).
                        log.info("AUTH_DEBUG: DENYING Unauthenticated HTTPS CONNECT from {}. URL={}", clientIp, url);
                        return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
                    }

                    // HTTP from unauthenticated: always redirect to portal (including probe URLs like /generate_204)
                    log.info("AUTH_DEBUG: REDIRECTING Unauthenticated IP {} to Portal. URL={}", clientIp, url);
                    HttpResponse redirect = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
                    redirect.headers().set(HttpHeaderNames.LOCATION, portalUrl);
                    return redirect;
                }

                // 3. Authenticated user: whitelist bypass (pass through whitelisted sites)
                if (isWhitelisted) {
                    log.debug("Whitelist Bypass (Authenticated) for {}: URL={}", clientIp, url);
                    return null;
                }

                // 4. Block HTTPS CONNECT to banned domains (gambling, gaming, torrent, adult)
                // Since MITM is disabled, we must block at the CONNECT tunnel level for HTTPS.
                if (request.method().equals(HttpMethod.CONNECT)) {
                    try {
                        // For CONNECT, the url is "host:443" — extract just the host
                        String connectHost = url.contains(":") ? url.split(":")[0] : url;
                        ClassificationRequest connectReq = new ClassificationRequest();
                        connectReq.setUrl("https://" + connectHost);
                        connectReq.setIpAddress(clientIp);
                        connectReq.setMethod("CONNECT");
                        connectReq.setUserAgent(request.headers().get(HttpHeaderNames.USER_AGENT));
                        ClassificationResult connectResult = trafficLogService.classify(connectReq);
                        if (connectResult != null && !connectResult.getIsAllowed()) {
                            log.info("BLOCKED HTTPS CONNECT: {} -> {} ({})", clientIp, connectHost, connectResult.getReason());
                            return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
                        }
                    } catch (Exception e) {
                        log.error("HTTPS CONNECT classification failed for {}: {}", clientIp, e.getMessage());
                    }
                    return null; // Allow if classification passes
                }

                // 5. Traffic Classification for HTTP (authenticated users on non-whitelisted sites)
                try {
                    ClassificationRequest req = new ClassificationRequest();
                    req.setUrl(url);
                    req.setIpAddress(clientIp);
                    req.setMethod(request.method().name());
                    req.setUserAgent(request.headers().get(HttpHeaderNames.USER_AGENT));
                    req.setReferer(request.headers().get(HttpHeaderNames.REFERER));

                    ClassificationResult result = trafficLogService.classify(req);
                    if (result != null && !result.getIsAllowed()) {
                        String blockedDomain = result.getDomain() != null ? result.getDomain() : "unknown";
                        String reason = result.getReason() != null ? result.getReason() : "Security Policy";
                        
                        String html = BLOCK_PAGE_HTML
                            .replace("{{DOMAIN}}", blockedDomain)
                            .replace("{{REASON}}", reason);

                        DefaultFullHttpResponse forbiddenResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                HttpResponseStatus.FORBIDDEN);
                        forbiddenResponse.content().writeBytes(html.getBytes(StandardCharsets.UTF_8));
                        forbiddenResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
                        forbiddenResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, forbiddenResponse.content().readableBytes());
                        return forbiddenResponse;
                    }
                } catch (Exception e) {
                    log.error("Traffic classification failed for {}: {}", clientIp, e.getMessage());
                }

                return null; // Proceed normal
            }
        });

        proxy.addResponseFilter(new ResponseFilter() {
            @Override
            public void filterResponse(HttpResponse response, HttpMessageContents contents,
                    HttpMessageInfo messageInfo) {
                if (contents == null || !contents.isText())
                    return;

                String contentType = response.headers().get(HttpHeaderNames.CONTENT_TYPE);
                if (contentType != null && contentType.toLowerCase().contains("text/html")) {
                    String html = contents.getTextContents();
                    String trackerScript = "<script id=\"iwacs-tracker\">\n" +
                            "(function() {\n" +
                            "    const apiEndpoint = \"/____iwacs_log\";\n" +
                            "    function sendEvent(type, details) {\n" +
                            "        fetch(apiEndpoint, {\n" +
                            "            method: 'POST',\n" +
                            "            headers: { 'Content-Type': 'text/plain' }, \n" +
                            "            body: JSON.stringify({ eventType: type, url: window.location.href, details: details, timestamp: new Date().toISOString() })\n"
                            +
                            "        }).catch(() => {});\n" +
                            "    }\n" +
                            "    document.addEventListener('click', (e) => {\n" +
                            "        const t = e.target.closest('a, button, input[type=\"submit\"]');\n" +
                            "        if (t) sendEvent('CLICK', (t.innerText || t.value || t.tagName).substring(0, 50).trim());\n"
                            +
                            "    }, true);\n" +
                            "})();\n" +
                            "</script>";

                    if (html.toLowerCase().contains("</body>")) {
                        html = html.replaceFirst("(?i)</body>", trackerScript + "</body>");
                        contents.setTextContents(html);
                    } else if (html.toLowerCase().contains("</html>")) {
                        html = html.replaceFirst("(?i)</html>", trackerScript + "</html>");
                        contents.setTextContents(html);
                    } else {
                        contents.setTextContents(html + trackerScript);
                    }
                }
            }
        });
    }


    private String extractDomainFromUrl(String url) {
        try {
            if (url == null) return "";
            String host = url;
            if (url.contains("://")) {
                host = new java.net.URL(url).getHost();
            } else if (url.contains("/")) {
                host = url.split("/")[0];
            }
            return host.toLowerCase();
        } catch (Exception e) {
            return url;
        }
    }
}
