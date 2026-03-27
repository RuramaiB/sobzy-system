package com.example.sobzybackend.service;

import org.springframework.stereotype.Service;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@org.springframework.stereotype.Service
public class EmbeddedDnsServer {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmbeddedDnsServer.class);

    private final PortalService portalService;
    private final ClassificationService classificationService;
    private String targetHostIp = "127.0.0.1";
    private DatagramSocket udpSocket;
    private boolean isRunning = false;

    public EmbeddedDnsServer(PortalService portalService, ClassificationService classificationService) {
        this.portalService = portalService;
        this.classificationService = classificationService;
    }

    private static final List<String> WPAD_DOMAINS = Arrays.asList("wpad.", "wpad.local.", "wpad.home.");

    public void setHostIp(String hostIp) {
        this.targetHostIp = hostIp;
        log.info("DNS Hijacker target IP updated to: {}", hostIp);
    }

    public void startDns(String hostIp) {
        if (hostIp != null && !hostIp.equals("0.0.0.0")) {
            this.targetHostIp = hostIp;
        }
        
        if (isRunning) {
            log.info("DNS Server is already running. Target IP: {}", targetHostIp);
            return;
        }
        isRunning = true;

        CompletableFuture.runAsync(() -> runDnsLoop());
    }

    public void stopDns() {
        isRunning = false;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        log.info("Embedded DNS Server stopped.");
    }

    private void runDnsLoop() {
        log.info("PRE-EMPTIVE DNS: Attempting to secure Port 53 on all interfaces...");
        
        int retries = 10; // Increased retries for front-running
        while (retries > 0 && isRunning) {
            try {
                // Bind to 0.0.0.0:53 (all interfaces) to catch requests robustly
                udpSocket = new DatagramSocket(53);
                log.info("SUCCESS: Port 53 secured. DNS Hijacker is now front-running.");
                break; // success
            } catch (Exception e) {
                retries--;
                log.warn("DNS Pre-bind failed ({} retries left). Conflict: {}", retries, e.getMessage());
                
                // If we failed, it might be ICS. SharedAccess registry fix should help,
                // but we wait a bit for it to stop if we just triggered a restart.
                if (retries > 0) {
                    try { Thread.sleep(3000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    log.error("CRITICAL: Port 53 is persistently locked.");
                    isRunning = false;
                    return;
                }
            }
        }

        try {
            byte[] receiveData = new byte[1024];
            while (isRunning) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                udpSocket.receive(receivePacket);
                
                String clientIp = receivePacket.getAddress().getHostAddress();
                
                try {
                    Message request = new Message(receivePacket.getData());
                    Record question = request.getQuestion();
                    if (question == null)
                        continue;

                    String qname = question.getName().toString();
                    Message response = new Message(request.getHeader().getID());
                    response.getHeader().setFlag(Flags.QR); // Response flag

                    // 1. Check WPAD
                    if (WPAD_DOMAINS.contains(qname.toLowerCase())) {
                        log.info("DNS WPAD RESOLVE: {} -> {}", clientIp, targetHostIp);
                        addARecord(response, question.getName(), targetHostIp);
                        sendResponse(response, udpSocket, receivePacket);
                        continue;
                    }

                    // 2. Check Authentication
                    boolean isAuth = portalService.isIpAuthenticated(clientIp);

                    // 3. Hijack Logic
                    if (!isAuth && !qname.contains("localhost") && !qname.contains(targetHostIp)) {
                        log.info("DNS HIJACK: {} -> {} (Target: {})", clientIp, targetHostIp, qname);
                        addARecord(response, question.getName(), targetHostIp);
                        sendResponse(response, udpSocket, receivePacket);
                    } else {
                        // Forward to real DNS (8.8.8.8) for authenticated users
                        forwardToUpstream(receivePacket.getData(), udpSocket, receivePacket.getAddress(),
                                receivePacket.getPort());
                    }

                } catch (Exception e) {
                    // Ignore malformed packets silently
                }
            }
        } catch (Exception e) {
            log.error("DNS Server loop error: {}", e.getMessage());
            isRunning = false;
        }
    }

    private void addARecord(Message response, Name name, String ip) throws Exception {
        response.addRecord(questionToQueryRecord(name), Section.QUESTION);
        ARecord aRecord = new ARecord(name, DClass.IN, 60, InetAddress.getByName(ip));
        response.addRecord(aRecord, Section.ANSWER);
    }

    private Record questionToQueryRecord(Name name) {
        return Record.newRecord(name, Type.A, DClass.IN);
    }

    private void sendResponse(Message response, DatagramSocket socket, DatagramPacket requestPacket) throws Exception {
        byte[] responseData = response.toWire();
        DatagramPacket responsePacket = new DatagramPacket(
                responseData, responseData.length, requestPacket.getAddress(), requestPacket.getPort());
        socket.send(responsePacket);
    }

    private void forwardToUpstream(byte[] queryData, DatagramSocket clientSocket, InetAddress clientAddress,
            int clientPort) {
        try {
            DatagramSocket upstreamSocket = new DatagramSocket();
            upstreamSocket.setSoTimeout(1000);

            InetAddress upstreamAddress = InetAddress.getByName("8.8.8.8");
            DatagramPacket forwardPacket = new DatagramPacket(queryData, queryData.length, upstreamAddress, 53);
            upstreamSocket.send(forwardPacket);

            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            upstreamSocket.receive(receivePacket);

            DatagramPacket toClientPacket = new DatagramPacket(
                    receivePacket.getData(), receivePacket.getLength(), clientAddress, clientPort);
            clientSocket.send(toClientPacket);

            upstreamSocket.close();
        } catch (Exception e) {
            log.debug("DNS Forward failed: {}", e.getMessage());
        }
    }
}
