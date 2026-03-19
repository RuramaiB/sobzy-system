package com.example.sobzybackend.service;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.BrowserUpProxyServer;
import com.browserup.bup.filters.RequestFilter;
import com.browserup.bup.filters.ResponseFilter;
import com.browserup.bup.util.HttpMessageContents;
import com.browserup.bup.util.HttpMessageInfo;
import com.browserup.bup.mitm.KeyStoreCertificateSource;
import com.browserup.bup.mitm.manager.ImpersonatingMitmManager;
import java.io.InputStream;
import java.security.KeyStore;
import org.springframework.core.io.ClassPathResource;
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

    private BrowserUpProxy proxy;
    private boolean isRunning = false;

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

            ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
                    .rootCertificateSource(certSource)
                    .build();

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

                // Traffic Classification check
                try {
                    ClassificationRequest req = new ClassificationRequest();
                    req.setUrl(url);
                    req.setIpAddress(clientIp);
                    req.setMethod(request.method().name());
                    req.setUserAgent(request.headers().get(HttpHeaderNames.USER_AGENT));
                    req.setReferer(request.headers().get(HttpHeaderNames.REFERER));

                    ClassificationResult result = trafficLogService.classify(req);
                    if (result != null && !result.getIsAllowed()) {
                        DefaultFullHttpResponse forbiddenResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                HttpResponseStatus.FORBIDDEN);
                        forbiddenResponse.content()
                                .writeBytes("Blocked by IWACS Policy".getBytes(StandardCharsets.UTF_8));
                        return forbiddenResponse;
                    }
                } catch (Exception e) {
                    log.error("Traffic classification failed: {}", e.getMessage());
                }

                // Captive portal block
                boolean isAuth = portalService.isIpAuthenticated(clientIp);
                if (!isAuth) {
                    if (url.contains("generate_204") || url.contains("ncsi")
                            || request.method().equals(HttpMethod.GET)) {
                        log.info("CAPTIVE PORTAL REDIRECT: {} -> {}", clientIp, portalUrl);
                        HttpResponse redirect = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                HttpResponseStatus.FOUND);
                        redirect.headers().set(HttpHeaderNames.LOCATION, portalUrl);
                        return redirect;
                    }
                } else {
                    log.debug("AUTHENTICATED TRAFFIC: {} -> {}", clientIp, url);
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
}
