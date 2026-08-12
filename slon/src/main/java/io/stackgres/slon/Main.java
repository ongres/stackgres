package io.stackgres.slon;

import io.stackgres.proto.slon.SlonStatus;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main implements Runnable {

    private static final String SLON_LOG_FILE = "/tmp/slon.log";

    private static final System.Logger logger = System.getLogger("Main");

    private final MatriarchClient client;
    private final UUID instanceId;
    private final Thread mainThread;

    public Main(UUID instanceId, String port) {
        this.instanceId = instanceId;
        this.mainThread = Thread.currentThread();
        this.client = new MatriarchClient(port, SlonStatus.STATUS_CREATED);
    }

    @Override
    public void run() {
        logger.log(System.Logger.Level.INFO, "Starting Slon instance {0}", instanceId);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.log(System.Logger.Level.INFO, "Shutting down...");
            client.initiateShutdown();
            try {
                mainThread.join(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            client.close();
            logger.log(System.Logger.Level.INFO, "Shutdown complete");
        }));

        client.connectToMatriarch(instanceId);
    }

    public static void main(String[] args) {
        configureLogFormat();
        configureJsonFileLogging();
        try {
            UUID uuid = UUID.fromString(args[0]);
            String port = args[1];
            new Main(uuid, port).run();
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Instance ID and port are required!");
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void configureLogFormat() {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] %4$s %3$s: %5$s%n");
    }

    private static void configureJsonFileLogging() {
        try {
            FileHandler fileHandler = new FileHandler(SLON_LOG_FILE, true);
            fileHandler.setFormatter(new JsonLogFormatter());
            Logger root = LogManager.getLogManager().getLogger("");
            root.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("Failed to attach JSON file handler at " + SLON_LOG_FILE + ": " + e.getMessage());
        }
    }

}