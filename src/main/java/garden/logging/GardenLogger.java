package garden.logging;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Singleton logger for file and in-memory simulation logs.
 */
public class GardenLogger {

    private static GardenLogger instance;
    private static final String LOG_DIR = "logs";
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private PrintWriter fileWriter;
    private final ConcurrentLinkedQueue<LogEntry> recentEntries;
    private final List<LogEntry> allEntries;
    private final String logFileName;

    public static class LogEntry {
        private final LocalDateTime timestamp;
        private final String category;
        private final String message;
        private final String level;

        public LogEntry(String level, String category, String message) {
            this.timestamp = LocalDateTime.now();
            this.level = level;
            this.category = category;
            this.message = message;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public String getCategory() { return category; }
        public String getMessage() { return message; }
        public String getLevel() { return level; }

        @Override
        public String toString() {
            return String.format("[%s] [%s] [%s] %s",
                    timestamp.format(TIMESTAMP_FMT), level, category, message);
        }
    }

    private GardenLogger() {
        recentEntries = new ConcurrentLinkedQueue<>();
        allEntries = Collections.synchronizedList(new ArrayList<>());

        new File(LOG_DIR).mkdirs();
        logFileName = LOG_DIR + "/garden_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + ".log";
        try {
            fileWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFileName, java.nio.charset.StandardCharsets.UTF_8, true)), false);
            fileWriter.println("=== GARDEN SIMULATION LOG ===");
            fileWriter.println("Started: " + LocalDateTime.now().format(TIMESTAMP_FMT));
            fileWriter.println("=".repeat(60));
            fileWriter.println();
        } catch (IOException e) {
            System.err.println("WARNING: Could not create log file: " + e.getMessage());
        }
    }

    public static synchronized GardenLogger getInstance() {
        if (instance == null) {
            instance = new GardenLogger();
        }
        return instance;
    }

    public void log(String category, String message) {
        LogEntry entry = new LogEntry("INFO", category, message);
        writeEntry(entry);
    }

    public void logWarning(String category, String message) {
        LogEntry entry = new LogEntry("WARN", category, message);
        writeEntry(entry);
    }

    public void logError(String category, String message, Exception e) {
        String fullMessage = message + " | Exception: " + e.getClass().getSimpleName()
                + " - " + e.getMessage();
        LogEntry entry = new LogEntry("ERROR", category, fullMessage);
        writeEntry(entry);
    }

    public void logError(String category, String message) {
        LogEntry entry = new LogEntry("ERROR", category, message);
        writeEntry(entry);
    }

    private static final int MAX_ALL_ENTRIES = 50_000;

    private synchronized void writeEntry(LogEntry entry) {
        allEntries.add(entry);
        recentEntries.add(entry);

        while (recentEntries.size() > 500) {
            recentEntries.poll();
        }

        if (allEntries.size() > MAX_ALL_ENTRIES) {
            int excess = allEntries.size() - MAX_ALL_ENTRIES;
            allEntries.subList(0, excess).clear();
        }

        if (fileWriter != null) {
            fileWriter.println(entry.toString());
            if (allEntries.size() % 50 == 0) {
                fileWriter.flush();
            }
        }
    }

    public List<LogEntry> getRecentEntries() {
        return new ArrayList<>(recentEntries);
    }

    public List<LogEntry> getAllEntries() {
        synchronized (allEntries) {
            return new ArrayList<>(allEntries);
        }
    }

    public List<LogEntry> getEntriesByCategory(String category) {
        synchronized (allEntries) {
            List<LogEntry> filtered = new ArrayList<>();
            for (LogEntry entry : allEntries) {
                if (entry.getCategory().equalsIgnoreCase(category)) {
                    filtered.add(entry);
                }
            }
            return filtered;
        }
    }

    public List<LogEntry> getEntriesByLevel(String level) {
        synchronized (allEntries) {
            List<LogEntry> filtered = new ArrayList<>();
            for (LogEntry entry : allEntries) {
                if (entry.getLevel().equalsIgnoreCase(level)) {
                    filtered.add(entry);
                }
            }
            return filtered;
        }
    }

    public String getLogFileName() {
        return logFileName;
    }

    public void close() {
        if (fileWriter != null) {
            fileWriter.println();
            fileWriter.println("=".repeat(60));
            fileWriter.println("Log closed: " + LocalDateTime.now().format(TIMESTAMP_FMT));
            fileWriter.close();
        }
    }
}
