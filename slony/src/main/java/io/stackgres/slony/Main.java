package io.stackgres.slony;

import io.stackgres.postgres.ClusterInstance;
import io.stackgres.slony.client.MatriarchClient;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.SimpleFormatter;

public class Main implements Runnable {

    private static final String SLONY_LOG_PATH = "/tmp/slony.log";

    private static final System.Logger logger = System.getLogger("Main");

    private final MatriarchClient client = new MatriarchClient();

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.log(System.Logger.Level.INFO, "Shutting down...");
            client.close();
        }));

        client.connectToMatriarch();
    }

    public static String getSlonyLogPath() {
        return SLONY_LOG_PATH;
    }

    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] %4$s %3$s: %5$s%n");
        try {
            FileHandler fileHandler = new FileHandler(SLONY_LOG_PATH, true);
            fileHandler.setFormatter(new SimpleFormatter());
            LogManager.getLogManager().getLogger("").addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("Failed to configure slony log file: " + e.getMessage());
        }

        new Main().run();
    }

}