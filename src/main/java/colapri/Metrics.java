package colapri;

import org.cloudsimplus.cloudlets.Cloudlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Per-class and global scheduling metrics for the ColaPri evaluation.
 *
 * For each cloudlet: arrival = getDcArrivalTime(), start = getStartTime(),
 * finish = getFinishTime(). waiting = start - arrival, response = finish - arrival.
 *
 * Weighted Jain fairness uses per-class weight = ColaPri repetition (0:60 .. 9:1),
 * i.e. each class "should" receive service proportional to its weight. A
 * weighted-fair scheduler -> J close to 1; strict priority or unweighted-fair -> low J.
 */
public final class Metrics {

    /** Per-class weight used by the fairness metric. Must match the scheduler's weights for the
     *  weighted-Jain to be meaningful. Default is monotonic; override with setClassWeights(). */
    private static final int[] CLASS_WEIGHT = {6, 5, 4, 3, 3, 3, 2, 2, 2, 1};

    public static void setClassWeights(final Map<Integer, Integer> w) {
        for (int p = 0; p < 10; p++) {
            final Integer x = w.get(p);
            if (x != null && x > 0) CLASS_WEIGHT[p] = x;
        }
    }

    /** mean and 95% confidence interval half-width (1.96*std/sqrt(n)) over a sample. */
    public static double[] meanCi95(final List<Double> v) {
        if (v.isEmpty()) return new double[]{0, 0};
        final double m = v.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        if (v.size() < 2) return new double[]{m, 0};
        double s2 = 0;
        for (final double x : v) s2 += (x - m) * (x - m);
        final double std = Math.sqrt(s2 / (v.size() - 1));
        return new double[]{m, 1.96 * std / Math.sqrt(v.size())};
    }

    public static int weightOf(final int priority) {
        final int p = priority < 0 ? 0 : (priority > 9 ? 9 : priority);
        return CLASS_WEIGHT[p];
    }

    private static double waiting(final Cloudlet c) { return Math.max(0, c.getStartTime() - c.getDcArrivalTime()); }
    private static double response(final Cloudlet c) { return Math.max(0, c.getFinishTime() - c.getDcArrivalTime()); }

    private static double mean(final List<Double> v) {
        return v.isEmpty() ? 0 : v.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private static double p95(final List<Double> v) {
        if (v.isEmpty()) return 0;
        final var s = new ArrayList<>(v);
        s.sort(Double::compareTo);
        return s.get((int) Math.floor(0.95 * (s.size() - 1)));
    }

    private static double max(final List<Double> v) {
        return v.stream().mapToDouble(Double::doubleValue).max().orElse(0);
    }

    public static Map<Integer, List<Cloudlet>> byPriority(final List<Cloudlet> finished) {
        final Map<Integer, List<Cloudlet>> m = new TreeMap<>();
        for (final var c : finished) m.computeIfAbsent(c.getPriority(), k -> new ArrayList<>()).add(c);
        return m;
    }

    public static double makespan(final List<Cloudlet> finished) {
        double min = Double.MAX_VALUE, maxF = 0;
        for (final var c : finished) {
            min = Math.min(min, c.getDcArrivalTime());
            maxF = Math.max(maxF, c.getFinishTime());
        }
        return finished.isEmpty() ? 0 : maxF - min;
    }

    // ---- per-class accessors (return raw numbers so a multi-seed harness can aggregate with meanCi95) ----

    private static List<Double> respOfClass(final List<Cloudlet> finished, final int c) {
        final var v = new ArrayList<Double>();
        for (final var x : finished) if (x.getPriority() == c) v.add(response(x));
        return v;
    }
    private static List<Double> waitOfClass(final List<Cloudlet> finished, final int c) {
        final var v = new ArrayList<Double>();
        for (final var x : finished) if (x.getPriority() == c) v.add(waiting(x));
        return v;
    }

    public static double meanResponseOfClass(final List<Cloudlet> finished, final int c) { return mean(respOfClass(finished, c)); }
    public static double p95ResponseOfClass(final List<Cloudlet> finished, final int c) { return p95(respOfClass(finished, c)); }
    public static double meanWaitOfClass(final List<Cloudlet> finished, final int c) { return mean(waitOfClass(finished, c)); }
    public static double maxWaitOfClass(final List<Cloudlet> finished, final int c) { return max(waitOfClass(finished, c)); }

    public static int starvationCount(final List<Cloudlet> finished, final int c, final double threshold) {
        int n = 0;
        for (final var x : finished) if (x.getPriority() == c && waiting(x) > threshold) n++;
        return n;
    }

    public static double throughput(final List<Cloudlet> finished) {
        final double mk = makespan(finished);
        return mk <= 0 ? 0 : finished.size() / mk;
    }

    /** Load imbalance across VMs = coefficient of variation (std/mean) of per-VM assigned work (MI). 0 = perfectly balanced. */
    public static double loadImbalanceCV(final List<? extends List<Cloudlet>> perVm) {
        final var loads = new ArrayList<Double>();
        for (final var vm : perVm) {
            double s = 0;
            for (final var c : vm) s += c.getLength();
            loads.add(s);
        }
        final double m = mean(loads);
        if (m <= 0) return 0;
        double s2 = 0;
        for (final double l : loads) s2 += (l - m) * (l - m);
        return Math.sqrt(s2 / loads.size()) / m;
    }

    /** Weighted Jain fairness over per-class (meanResponse * weight). Under ideal
     *  weighted-fairness response is inversely proportional to weight, so the product is
     *  ~constant across classes -> J ~ 1; strict priority or unweighted -> J < 1.
     *  (Throughput-based Jain is useless here: every cloudlet eventually completes.) */
    public static double jainWeighted(final List<Cloudlet> finished) {
        final var groups = byPriority(finished);
        final List<Double> x = new ArrayList<>();
        for (final var e : groups.entrySet()) {
            final double meanResp = e.getValue().stream().mapToDouble(Metrics::response).average().orElse(0);
            x.add(meanResp * weightOf(e.getKey()));
        }
        double sum = 0, sumSq = 0;
        for (final double xi : x) { sum += xi; sumSq += xi * xi; }
        return x.isEmpty() || sumSq == 0 ? 0 : (sum * sum) / (x.size() * sumSq);
    }

    /** Prints the per-class table and the global summary for one scheduler run. */
    public static void report(final String name, final List<Cloudlet> finished, final int starvationThreshold) {
        System.out.printf("%n--- %s ---%n", name);
        System.out.printf("  %-6s %-5s %10s %10s %10s %10s %8s%n",
            "class", "n", "resp.mean", "resp.p95", "wait.mean", "wait.MAX", "starv");
        for (final var e : byPriority(finished).entrySet()) {
            final var resp = new ArrayList<Double>();
            final var wait = new ArrayList<Double>();
            int starved = 0;
            for (final var c : e.getValue()) {
                resp.add(response(c));
                final double w = waiting(c);
                wait.add(w);
                if (w > starvationThreshold) starved++;
            }
            System.out.printf("  p%-5d %-5d %10.1f %10.1f %10.1f %10.1f %8d%n",
                e.getKey(), e.getValue().size(), mean(resp), p95(resp), mean(wait), max(wait), starved);
        }
        System.out.printf("  GLOBAL: makespan=%.1f  throughput=%.3f/s  JainWeighted=%.3f%n",
            makespan(finished), finished.size() / Math.max(1e-9, makespan(finished)), jainWeighted(finished));
    }

    /** Compact summary line for cross-scheduler comparison tables. */
    public static String summaryLine(final String name, final List<Cloudlet> finished, final int highPri, final int lowPri) {
        final var g = byPriority(finished);
        final double respHigh = g.containsKey(highPri) ? mean(resps(g.get(highPri))) : 0;
        final double waitLowMax = g.containsKey(lowPri) ? max(waits(g.get(lowPri))) : 0;
        return String.format("%-26s respHigh(p%d)=%7.1f  waitMAX_low(p%d)=%8.1f  Jain=%.3f  makespan=%6.1f",
            name, highPri, respHigh, lowPri, waitLowMax, jainWeighted(finished), makespan(finished));
    }

    private static List<Double> resps(final List<Cloudlet> cs) {
        final var v = new ArrayList<Double>();
        for (final var c : cs) v.add(response(c));
        return v;
    }
    private static List<Double> waits(final List<Cloudlet> cs) {
        final var v = new ArrayList<Double>();
        for (final var c : cs) v.add(waiting(c));
        return v;
    }
}
