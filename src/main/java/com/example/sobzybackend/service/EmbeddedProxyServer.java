package com.example.sobzybackend.service;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.filters.RequestFilter;
import com.browserup.bup.filters.ResponseFilter;
import com.browserup.bup.util.HttpMessageContents;
import com.browserup.bup.util.HttpMessageInfo;
import org.littleshoot.proxy.MitmManager;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import com.example.sobzybackend.service.PortalService;
import com.example.sobzybackend.service.ClassificationService;
import com.example.sobzybackend.service.TrafficLogService;
import com.browserup.bup.mitm.KeyStoreCertificateSource;
import com.browserup.bup.mitm.manager.ImpersonatingMitmManager;
import org.springframework.core.io.ClassPathResource;
import java.io.InputStream;
import java.security.KeyStore;

import com.example.sobzybackend.dtos.ClassificationRequest;
import com.example.sobzybackend.dtos.ClassificationResult;
import io.netty.handler.codec.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddedProxyServer {

    private final PortalService portalService;
    private final TrafficLogService trafficLogService;
    private final ClassificationService classificationService;

    private BrowserUpProxy proxy;
    private boolean isRunning = false;

    private static final String BLOCK_PAGE_HTML = "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>Access Restricted | IWACS</title><link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;600;800&display=swap' rel='stylesheet'><style>:root{--bg:#0a0a0a;--card-bg:#141414;--text-primary:#fff;--text-secondary:#a1a1aa;--accent:#ef4444;--border:#27272a}*{margin:0;padding:0;box-sizing:border-box}body{background-color:var(--bg);color:var(--text-primary);font-family:Inter,-apple-system,sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;overflow:hidden}.container{max-width:500px;width:90%;text-align:center;background:var(--card-bg);padding:3rem 2rem;border-radius:24px;border:1px solid var(--border);box-shadow:0 25px 50px -12px rgba(0,0,0,.5);animation:fadeIn .6s cubic-bezier(.16,1,.3,1)}@keyframes fadeIn{from{opacity:0;transform:translateY(20px)}to{opacity:1;transform:translateY(0)}}.icon-box{background:rgba(239,68,68,.1);width:80px;height:80px;border-radius:20px;display:flex;align-items:center;justify-content:center;margin:0 auto 1.5rem;color:var(--accent)}h1{font-size:1.875rem;font-weight:800;margin-bottom:.75rem;letter-spacing:-.025em}p{color:var(--text-secondary);line-height:1.6;margin-bottom:2rem}.details{background:rgba(255,255,255,.03);border:1px solid var(--border);border-radius:12px;padding:1rem;margin-bottom:2rem;text-align:left}.detail-row{display:flex;justify-content:space-between;margin-bottom:.5rem;font-size:.875rem}.detail-row:last-child{margin-bottom:0}.label{color:var(--text-secondary)}.value{color:var(--text-primary);font-weight:600;word-break:break-all}.btn{display:inline-block;background:var(--text-primary);color:var(--bg);padding:.75rem 2rem;border-radius:12px;font-weight:600;text-decoration:none;transition:transform .2s,background .2s}.btn:hover{background:#e4e4e7;transform:translateY(-2px)}.footer{margin-top:2rem;font-size:.75rem;color:#52525b}</style></head><body><div class='container'><div class='icon-box'><svg xmlns='http://www.w3.org/2000/svg' width='40' height='40' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'><path d='M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z'></path><line x1='12' y1='8' x2='12' y2='12'></line><line x1='12' y1='16' x2='12.01' y2='16'></line></svg></div><h1>Access Restricted</h1><p>Your request to access this website has been blocked due to network security policies.</p><div class='details'><div class='detail-row'><span class='label'>Website</span><span class='value'>{{DOMAIN}}</span></div><div class='detail-row'><span class='label'>Reason</span><span class='value'>{{REASON}}</span></div></div><a href='javascript:history.back()' class='btn'>Go Back</a><div class='footer'>Powered by IWACS Intelligence Systems</div></div></body></html>";

    public void startProxy(String hostIp) {
        if (isRunning)
            return;

        try {
            proxy = new BrowserUpProxyServer();
            proxy.setTrustAllServers(true);
            // Configure MITM with custom CA from classpath-loaded PKCS12 keystore
            InputStream is = new ClassPathResource("iwacs-ca.p12").getInputStream();
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(is, "password".toCharArray());

            KeyStoreCertificateSource certSource = new KeyStoreCertificateSource(ks, "iwacs-ca", "password");

            ImpersonatingMitmManager baseMitmManager = ImpersonatingMitmManager.builder()
                    .rootCertificateSource(certSource)
                    .build();

            // Use our custom WhitelistedMitmManager
            MitmManager mitmManager = new WhitelistedMitmManager(baseMitmManager, portalService, classificationService);

            proxy.setMitmManager(mitmManager);
            proxy.setMitmDisabled(false);

            // Listen on port 8080
            proxy.start(8080);

            // Inject Request and Response filters
            configureFilters(hostIp);

            isRunning = true;
            log.info("Embedded MITM Proxy started successfully on {}:8080", hostIp);
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
                if (url.contains(hostIp) || url.contains("localhost")
                        || (hostHeader != null && (hostHeader.contains(hostIp) || hostHeader.contains(":1998")
                                || hostHeader.contains(":3000")))) {
                    log.debug("Proxy Bypassed for portal/local traffic: URI={}, Host={}", url, hostHeader);
                    return null;
                }

                // 2. Captive portal check (Redirect unauthenticated users)
                boolean isAuth = portalService.isIpAuthenticated(clientIp);
                
                // Only redirect HTTP GET/HEAD requests to the portal.
                // Redirecting CONNECT or other methods during handshake leads to SSL errors.
                if (!isAuth && !request.method().equals(HttpMethod.CONNECT)) {
                    log.info("AUTH_DEBUG: REDIRECTING Unauthenticated IP {} to Portal. URL={}", clientIp, url);
                    HttpResponse redirect = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
                    redirect.headers().set(HttpHeaderNames.LOCATION, portalUrl);
                    return redirect;
                }

                // 3. Whitelist Bypass for Authenticated Users (to avoid SSL errors on trusted sites)
                String host = hostHeader != null ? hostHeader.split(":")[0] : "";
                if (classificationService.isWhitelisted(host)) {
                    log.debug("Whitelist Bypass for {}: URL={}", clientIp, url);
                    return null;
                }

                // 4. Traffic Classification (for authenticated users on non-whitelisted sites)
                try {
                    ClassificationRequest req = new ClassificationRequest();
                    req.setUrl(url);
                    req.setIpAddress(clientIp);
                    req.setMethod(request.method().name());
                    req.setUserAgent(request.headers().get(HttpHeaderNames.USER_AGENT));
                    req.setReferer(request.headers().get(HttpHeaderNames.REFERER));

                    ClassificationResult result = trafficLogService.classify(req);
                    if (result != null && !result.getIsAllowed()) {
                        String domain = result.getDomain() != null ? result.getDomain() : "unknown";
                        String reason = result.getReason() != null ? result.getReason() : "Security Policy";
                        
                        String html = BLOCK_PAGE_HTML
                            .replace("{{DOMAIN}}", domain)
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

    @RequiredArgsConstructor
    private static class WhitelistedMitmManager implements MitmManager {
        private final ImpersonatingMitmManager delegate;
        private final PortalService portalService;
        private final ClassificationService classificationService;

        @Override
        public SSLEngine serverSslEngine(String peerHost, int peerPort) {
            return delegate.serverSslEngine(peerHost, peerPort);
        }

        @Override
        public SSLEngine serverSslEngine() {
            return delegate.serverSslEngine();
        }

        @Override
        public SSLEngine clientSslEngineFor(HttpRequest request, SSLSession session) {
            try {
                String hostHeader = request.headers().get(HttpHeaderNames.HOST);
                String host = hostHeader != null ? hostHeader.split(":")[0] : "";
                
                // 1. Bypass for whitelisted domains (to avoid cert errors on Google, etc.)
                if (classificationService.isWhitelisted(host)) {
                    log.debug("SSL Bypass (MITM Disabled) for Whitelisted Host: {}", host);
                    return null; 
                }

                return delegate.clientSslEngineFor(request, session);
            } catch (Exception e) {
                log.error("Error in WhitelistedMitmManager: {}", e.getMessage());
                return delegate.clientSslEngineFor(request, session);
            }
        }
    }
}
