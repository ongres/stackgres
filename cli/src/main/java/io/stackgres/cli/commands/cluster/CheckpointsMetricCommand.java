package io.stackgres.cli.commands.cluster;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.proto.cli.Checkpoint;
import io.stackgres.proto.cli.ClusterCheckpoints;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Command(name = "checkpoints", description = "Show Postgres checkpoint activity for a cluster (frequency, duration, I/O)")
public class CheckpointsMetricCommand extends StackGresSubCommand {

    enum Format {summary, table, json, sparkline}

    private static final DateTimeFormatter TS_DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final char[] SPARK_BLOCKS = {'▁', '▂', '▃', '▄', '▅', '▆', '▇', '█'};
    private static final int SPARK_WIDTH = 40;

    private final MatriarchClient client = new MatriarchClient();

    @Parameters(description = "The cluster name")
    String name;

    @Option(names = {"-i", "--instance"}, description = "The PostgreSQL instance name for high available clusters (default: merge across all instances)")
    String instanceName;

    @Option(names = {"-d", "--database"}, description = "The Postgres database to filter")
    String database;

    @Option(names = "--since", description = "Start of time window. Relative (1h, 24h, 7d) or ISO-8601. Default: 24h")
    String since = "24h";

    @Option(names = "--until", description = "End of time window. Same format as --since. Default: now")
    String until;

    @Option(names = "--limit", description = "Max checkpoints to fetch. Default: 200")
    int limit = 200;

    @Option(names = "--format", description = "Output format: ${COMPLETION-CANDIDATES}. Default: summary")
    Format format = Format.summary;

    @Override
    public void run() {
        if (debug) client.setDebug();

        Instant now = Instant.now();
        Instant start = parseWhen(since, now);
        Instant end = until == null ? now : parseWhen(until, now);
        if (!end.isAfter(start))
            throw new IllegalArgumentException("--until must be after --since");

        ClusterCheckpoints result = client.getClusterCheckpoints(name, instanceName, database, start, end, limit);
        List<Checkpoint> checkpoints = result.getCheckpointList();

        // Server returns DESC by default; render oldest-first for time-series math.
        Checkpoint[] sorted = checkpoints.toArray(new Checkpoint[0]);
        Arrays.sort(sorted, Comparator.comparing(c -> Instant.ofEpochSecond(c.getTimestamp().getSeconds(), c.getTimestamp().getNanos())));

        switch (format) {
            case summary -> renderSummary(sorted, start, end, result.getTruncated());
            case table -> renderTable(sorted);
            case json -> renderJson(result);
            case sparkline -> renderSparkline(sorted, start, end);
        }
    }

    private void renderSummary(Checkpoint[] cs, Instant start, Instant end, boolean truncated) {
        Duration window = Duration.between(start, end);
        outln(String.format("checkpoints — %s — %s", name, instanceName == null ? "all instances" : "instance " + instanceName));
        outln(String.format("window: %s → %s (%s)", TS_DISPLAY.format(start), TS_DISPLAY.format(end), humanDuration(window)));
        outln("");

        if (cs.length == 0) {
            outln("  no checkpoints in this window");
            return;
        }

        double[] totals = new double[cs.length];
        double[] writes = new double[cs.length];
        double[] syncs = new double[cs.length];
        long totalBuffers = 0;
        for (int i = 0; i < cs.length; i++) {
            totals[i] = cs[i].getTotalSeconds();
            writes[i] = cs[i].getWriteSeconds();
            syncs[i] = cs[i].getSyncSeconds();
            totalBuffers += cs[i].getBuffersWritten();
        }

        double avgTotal = avg(totals);
        double sumWrite = sum(writes), sumSync = sum(syncs), sumTotal = sum(totals);
        double writePct = sumTotal > 0 ? 100.0 * sumWrite / sumTotal : 0;
        double syncPct = sumTotal > 0 ? 100.0 * sumSync / sumTotal : 0;
        double otherPct = Math.max(0, 100.0 - writePct - syncPct);

        long avgIntervalMs = cs.length > 1
                ? Duration.between(tsOf(cs[0]), tsOf(cs[cs.length - 1])).toMillis() / (cs.length - 1)
                : 0;
        long minIntervalMs = Long.MAX_VALUE, maxIntervalMs = 0;
        for (int i = 1; i < cs.length; i++) {
            long g = Duration.between(tsOf(cs[i - 1]), tsOf(cs[i])).toMillis();
            minIntervalMs = Math.min(minIntervalMs, g);
            maxIntervalMs = Math.max(maxIntervalMs, g);
        }

        outf("  count               %d%s%n", cs.length, truncated ? "  (truncated — increase --limit)" : "");
        if (cs.length > 1) {
            outf("  fire interval       avg %s  min %s  max %s%n", humanMs(avgIntervalMs), humanMs(minIntervalMs), humanMs(maxIntervalMs));
        }
        outf("  total duration      avg %s  p95 %s  max %s%n", humanSec(avgTotal), humanSec(percentile(totals, 95)), humanSec(max(totals)));
        outf("  write / sync split  write %.0f%%  sync %.0f%%  other %.0f%%%s%n", writePct, syncPct, otherPct, hint(syncPct, writePct));
        outf("  buffers written     avg %s  total %s (~%s)%n",
                String.format("%,d", (long) avg(toLongs(cs, Checkpoint::getBuffersWritten))),
                String.format("%,d", totalBuffers),
                humanBytes(totalBuffers * 8L * 1024L));
        outln("");
        outln("  duration distribution (s, log-bucketed)");
        renderDurationHistogram(totals);
        outln("");
        outln("  checkpoint timeline (▁..█ = duration percentile within window)");
        outln("  " + buildTimeline(cs, start, end));
        outln(String.format("  %s ago%s%s now", humanDuration(window), " ".repeat(Math.max(1, SPARK_WIDTH - humanDuration(window).length() - 8)), ""));
        outln("");
        outln("  most recent");
        outf("    %-19s  %-12s  %8s  %8s  %8s  %8s%n", "TIMESTAMP", "DATABASE", "BUFFERS", "WRITE", "SYNC", "TOTAL");
        int recent = Math.min(10, cs.length);
        for (int i = cs.length - 1; i >= cs.length - recent; i--) {
            Checkpoint c = cs[i];
            outf("    %-19s  %-12s  %8d  %8s  %8s  %8s%n",
                    TS_DISPLAY.format(tsOf(c)),
                    truncate(c.getDatabase(), 12),
                    c.getBuffersWritten(),
                    humanSec(c.getWriteSeconds()),
                    humanSec(c.getSyncSeconds()),
                    humanSec(c.getTotalSeconds()));
        }
    }

    private void renderTable(Checkpoint[] cs) {
        outf("%-20s  %-12s  %8s  %8s  %8s  %8s%n", "TIMESTAMP", "DATABASE", "BUFFERS", "WRITE", "SYNC", "TOTAL");
        for (Checkpoint c : cs) {
            outf("%-20s  %-12s  %8d  %8s  %8s  %8s%n",
                    TS_DISPLAY.format(tsOf(c)),
                    truncate(c.getDatabase(), 12),
                    c.getBuffersWritten(),
                    humanSec(c.getWriteSeconds()),
                    humanSec(c.getSyncSeconds()),
                    humanSec(c.getTotalSeconds()));
        }
    }

    private void renderJson(ClusterCheckpoints result) {
        try {
            outln(JsonFormat.printer().includingDefaultValueFields().print(result));
        } catch (Exception e) {
            throw new RuntimeException("Failed to render JSON: " + e.getMessage(), e);
        }
    }

    private void renderSparkline(Checkpoint[] cs, Instant start, Instant end) {
        Duration window = Duration.between(start, end);
        outf("checkpoints %s (%s): %d fires", name, humanDuration(window), cs.length);
        if (cs.length > 0) {
            double[] totals = new double[cs.length];
            for (int i = 0; i < cs.length; i++) totals[i] = cs[i].getTotalSeconds();
            outf(", avg %s — %s", humanSec(avg(totals)), buildTimeline(cs, start, end));
        }
        outln("");
    }

    private void renderDurationHistogram(double[] totals) {
        // Log-buckets that match the DBA's mental model.
        double[] edges = {0.05, 0.1, 0.5, 1.0, Double.POSITIVE_INFINITY};
        String[] labels = {"<0.05", "<0.1 ", "<0.5 ", "<1.0 ", "≥1.0 "};
        int[] counts = new int[edges.length];
        for (double t : totals) {
            for (int i = 0; i < edges.length; i++) {
                if (t < edges[i]) {
                    counts[i]++;
                    break;
                }
            }
        }
        int maxCount = 0;
        for (int c : counts) maxCount = Math.max(maxCount, c);
        for (int i = 0; i < edges.length; i++) {
            int barLen = maxCount == 0 ? 0 : (int) Math.round(24.0 * counts[i] / maxCount);
            outf("    %s %s %d%n", labels[i], "█".repeat(barLen), counts[i]);
        }
    }

    private String buildTimeline(Checkpoint[] cs, Instant start, Instant end) {
        if (cs.length == 0) return " ".repeat(SPARK_WIDTH);
        double windowSec = Duration.between(start, end).toMillis() / 1000.0;
        if (windowSec <= 0) return " ".repeat(SPARK_WIDTH);
        // Bucket by time, hold the max total_seconds in each bucket.
        double[] bucketMax = new double[SPARK_WIDTH];
        for (Checkpoint c : cs) {
            double offset = (tsOf(c).toEpochMilli() - start.toEpochMilli()) / 1000.0;
            int b = Math.min(SPARK_WIDTH - 1, Math.max(0, (int) (offset / windowSec * SPARK_WIDTH)));
            bucketMax[b] = Math.max(bucketMax[b], c.getTotalSeconds());
        }
        double maxVal = 0;
        for (double v : bucketMax) maxVal = Math.max(maxVal, v);
        StringBuilder sb = new StringBuilder();
        for (double v : bucketMax) {
            if (v == 0) {
                sb.append(' ');
                continue;
            }
            int idx = maxVal == 0 ? 0 : (int) Math.round((SPARK_BLOCKS.length - 1) * (v / maxVal));
            sb.append(SPARK_BLOCKS[Math.min(SPARK_BLOCKS.length - 1, Math.max(0, idx))]);
        }
        return sb.toString();
    }

    private static Instant tsOf(Checkpoint c) {
        Timestamp t = c.getTimestamp();
        return Instant.ofEpochSecond(t.getSeconds(), t.getNanos());
    }

    private static Instant parseWhen(String s, Instant now) {
        if (s == null || s.isBlank()) return now;
        // Relative: digits + (s|m|h|d|w)
        if (s.matches("\\d+[smhdw]")) {
            long n = Long.parseLong(s.substring(0, s.length() - 1));
            Duration d = switch (s.charAt(s.length() - 1)) {
                case 's' -> Duration.ofSeconds(n);
                case 'm' -> Duration.ofMinutes(n);
                case 'h' -> Duration.ofHours(n);
                case 'd' -> Duration.ofDays(n);
                case 'w' -> Duration.ofDays(n * 7);
                default -> throw new IllegalArgumentException("Unknown unit in --since/--until: " + s);
            };
            return now.minus(d);
        }
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("--since/--until must be relative (e.g. 24h, 7d) or ISO-8601 (e.g. 2026-05-18T00:00:00Z); got: " + s);
        }
    }

    private static double avg(double[] xs) {
        if (xs.length == 0) return 0;
        return sum(xs) / xs.length;
    }

    private static double avg(long[] xs) {
        if (xs.length == 0) return 0;
        long total = 0;
        for (long x : xs) total += x;
        return (double) total / xs.length;
    }

    private static double sum(double[] xs) {
        double s = 0;
        for (double x : xs) s += x;
        return s;
    }

    private static double max(double[] xs) {
        double m = Double.NEGATIVE_INFINITY;
        for (double x : xs) m = Math.max(m, x);
        return xs.length == 0 ? 0 : m;
    }

    private static double percentile(double[] xs, int p) {
        if (xs.length == 0) return 0;
        double[] copy = xs.clone();
        Arrays.sort(copy);
        int idx = (int) Math.round((p / 100.0) * (copy.length - 1));
        return copy[Math.min(copy.length - 1, Math.max(0, idx))];
    }

    private static long[] toLongs(Checkpoint[] cs, java.util.function.ToLongFunction<Checkpoint> f) {
        long[] out = new long[cs.length];
        for (int i = 0; i < cs.length; i++) out[i] = f.applyAsLong(cs[i]);
        return out;
    }

    private static String hint(double syncPct, double writePct) {
        if (syncPct >= 70) return "\n                      └─ sync-bound; check disk fsync throughput";
        if (writePct >= 70) return "\n                      └─ write-bound; CPU/memory pressure on bgwriter";
        return "";
    }

    private static String humanSec(double s) {
        if (s < 0.001) return String.format("%dµs", (long) (s * 1_000_000));
        if (s < 1) return String.format("%.3fs", s);
        if (s < 60) return String.format("%.2fs", s);
        long secs = (long) s;
        long mins = secs / 60;
        return mins + "m" + (secs % 60) + "s";
    }

    private static String humanMs(long ms) {
        if (ms < 1000) return ms + "ms";
        return humanSec(ms / 1000.0);
    }

    private static String humanDuration(Duration d) {
        long s = d.getSeconds();
        if (s < 60) return s + "s";
        if (s < 3600) return (s / 60) + "m" + (s % 60 == 0 ? "" : (s % 60) + "s");
        if (s < 86400) return (s / 3600) + "h" + (s % 3600 == 0 ? "" : ((s % 3600) / 60) + "m");
        return (s / 86400) + "d" + (s % 86400 == 0 ? "" : ((s % 86400) / 3600) + "h");
    }

    private static String humanBytes(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KiB", b / 1024.0);
        if (b < 1024L * 1024 * 1024) return String.format("%.1f MiB", b / (1024.0 * 1024));
        return String.format("%.1f GiB", b / (1024.0 * 1024 * 1024));
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

}