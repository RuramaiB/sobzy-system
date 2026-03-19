package com.example.sobzybackend.controllers;

import com.example.sobzybackend.service.PortalService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

@Slf4j
@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class RedirectionController {

    private final PortalService portalService;

    /**
     * Catch-all for Port 80 traffic hijacked via DNS.
     * Redirects unauthenticated users to the login portal.
     */
    @GetMapping(value = "/**", produces = "text/html")
    public Object handleRedirection(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        String url = request.getRequestURL().toString();
        String host = request.getHeader("Host");

        // 1. Skip redirection for portal/api itself OR if it's the host machine
        // (loopback)
        if (clientIp.equals("127.0.0.1") || clientIp.equals("0:0:0:0:0:0:0:1") || clientIp.equals("localhost")) {
            log.debug("Skipping redirection for local host: {}", clientIp);
            return null;
        }

        if (host != null && (host.contains("localhost") || host.contains("127.0.0.1") || host.contains("192.168.137.1")
                || host.contains("172.24."))) {
            if (url.contains("/login") || url.contains("/api") || url.contains("/_nuxt")
                    || url.contains("/favicon.ico")) {
                return null; // Let Spring handle it normally
            }
        }

        // 2. Check if authenticated
        if (!portalService.isIpAuthenticated(clientIp)) {
            log.info("[*] Redirection Triggered for IP: {} attempting to reach: {}", clientIp, url);

            // Dynamically determine the redirection target (Hotspot Gateway)
            String redirectTarget = "http://172.24.64.1:3000/login"; // Default fallback

            // Try to find the actual interface IP
            try {
                String serverName = request.getServerName();
                if (serverName.startsWith("172.24.") || serverName.startsWith("192.168.137.")) {
                    redirectTarget = "http://" + serverName + ":3000/login";
                }
            } catch (Exception e) {
            }

            return new RedirectView(redirectTarget);
        }

        return null; // Already authenticated, let traffic flow (though usually DNS would no longer
                     // hijack)
    }
}
