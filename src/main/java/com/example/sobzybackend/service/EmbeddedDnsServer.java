package com.example.sobzybackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddedDnsServer {

    private final PortalService portalService;
    private DatagramSocket udpSocket;
    private boolean isRunning = false;

    private static final List<String> WPAD_DOMAINS = Arrays.asList("wpad.", "wpad.local.", "wpad.home.");

    public void startDns(String hostIp) {
        if (isRunning)
            return;
        isRunning = true;

        CompletableFuture.runAsync(() -> runDnsLoop(hostIp));
    }

    public void stopDns() {
        isRunning = false;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        log.info("Embedded DNS Server stopped.");
    }

    private void runDnsLoop(String hostIp) {
        log.info("Starting DNS Hijacker on {}:53", hostIp);
        try {
            udpSocket = new DatagramSocket(53, InetAddress.getByName(hostIp));
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
                        log.info("DNS WPAD RESOLVE: {} -> {}", clientIp, hostIp);
                        addARecord(response, question.getName(), hostIp);
                        sendResponse(response, udpSocket, receivePacket);
                        continue;
                    }

                    // 2. Check Authentication
                    boolean isAuth = portalService.isIpAuthenticated(clientIp);

                    // 3. Hijack Logic
                    if (!isAuth && !qname.contains("localhost") && !qname.contains(hostIp)) {
                        addARecord(response, question.getName(), hostIp);
                        sendResponse(response, udpSocket, receivePacket);
                    } else {
                        // Forward to real DNS (8.8.8.8)
                        forwardToUpstream(receivePacket.getData(), udpSocket, receivePacket.getAddress(),
                                receivePacket.getPort());
                    }

                } catch (Exception e) {
                    // Ignore malformed packets silently to avoid log spam
                }
            }
        } catch (Exception e) {
            log.error("DNS Server failed to bind or encountered error: {}", e.getMessage());
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
