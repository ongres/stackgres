package io.stackgres.slony;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Networks {

    private static final Map<Integer, Instant> registeredPorts = new ConcurrentHashMap<>();

    public static int nextUnusedPort(int desiredPort) {
        int unusedPort = selectUnusedPort(desiredPort, -1);
        registeredPorts.put(unusedPort, Instant.now());
        return unusedPort;
    }

    private static int selectUnusedPort(int portStart, int portInc) {
        int port = portStart;
        while (port < 0xFFFF && port > 0) {
            if (!isPortUsed(port))
                return port;
            port = port + portInc;
        }
        throw new IllegalArgumentException("Could not find a free port (for scheme " + portStart + "," + (portStart + portInc) + ",...)");
    }

    private static boolean isPortUsed(int port) {
        if (registeredPorts.containsKey(port)) {
            Instant reserved = registeredPorts.get(port);
            Instant threshold = Instant.now().minus(30, ChronoUnit.MINUTES);
            if (threshold.isBefore(reserved))
                return true;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 100);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void freeReservedPort(int port) {
        registeredPorts.remove(port);
    }

}