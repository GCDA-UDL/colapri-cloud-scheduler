package colapri;

import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * EXP-F: how to set ColaPri's activation degree automatically. Compares five activation policies under
 * the SAME ColaPri mechanism on a NON-STATIONARY workload (calm -> storm -> calm), where no single
 * fixed threshold is best across phases:
 *
 *   always-on, FCFS (theta = infinity), fixed-theta, analytic(ramp), fuzzy (zero-order Sugeno:
 *   singleton outputs, weighted average), learned(offline lookup).
 *
 * Every hyper-parameter (the lookup table, the best fixed theta, the ramp placement of the analytic
 * and fuzzy controllers) is selected on TRAIN seeds; all policies are then evaluated on disjoint
 * TEST seeds. Objective: low high-class response without starving the low class
 * (cost = resp.p0 + 10 * starv.p9).
 */
public class ActivationEval {
    private static final int MIPS = 1000, VM_PES = 2;
    private static final long BASE_LEN = 4000;
    private static final double STARV_THRESHOLD = 60.0;
    private static final int N_BUCKETS = 6, BUCKET_W = 12;     // learned table over backlog buckets
    private static final int TRAIN = 10, TEST = 20;            // seeds 0..9 train, 10..29 test
    private static final int W_HIGH = 6, W_LOW = 1;            // class weights (0:high, 9:low)

    private record Result(List<Cloudlet> finished, double wallMs) {}

    public static void main(String[] args) {
        org.cloudsimplus.util.Log.setLevel(ch.qos.logback.classic.Level.OFF);
        Metrics.setClassWeights(Map.of(0, W_HIGH, 9, W_LOW));

        System.out.println("##########################################################################");
        System.out.println("#  EXP-F · ColaPri AUTOMATIC activation (non-stationary load: calm->storm->calm)");
        System.out.println("#  cost = resp.p0 + 10*starv.p9   (low = fast high-class, no low-class starvation)");
        System.out.println("##########################################################################");

        // ---- learn the lookup table offline on TRAIN seeds ----
        final double[] table = learnTable();
        System.out.print("\nLearned alpha by backlog bucket [each " + BUCKET_W + " tasks]: ");
        System.out.println(Arrays.toString(round(table)));

        // ---- best single fixed-theta: SELECTED on TRAIN seeds (grid incl. FCFS = theta infinity);
        //      the TEST cost is printed alongside for reporting only ----
        final int INF = Integer.MAX_VALUE;
        int bestTheta = 0; double bestThetaCost = Double.MAX_VALUE;
        System.out.println("\n[fixed-theta sweep] theta -> cost on TRAIN seeds (selection) | cost on TEST seeds (report)");
        for (final int th : new int[]{0, 5, 10, 20, 40, 60, 80, 160, INF}) {
            final double cTr = avgCostTrain(() -> ActivationPolicy.fixed(th));
            final double cTe = avgCostTest(() -> ActivationPolicy.fixed(th));
            System.out.printf("  theta=%-10s -> train %.1f | test %.1f%n", thetaName(th), cTr, cTe);
            if (cTr < bestThetaCost) { bestThetaCost = cTr; bestTheta = th; }
        }
        final int bt = bestTheta;
        System.out.printf("  best fixed theta on TRAIN = %s (train cost %.1f)%n", thetaName(bt), bestThetaCost);

        // ---- ramp placement (lo,hi) for the analytic and fuzzy controllers: SELECTED on TRAIN seeds ----
        final int[][] ramps = {{5, 40}, {10, 40}, {20, 50}, {30, 60}, {40, 80}};
        int[] bestA = ramps[0], bestF = ramps[0]; double bestACost = Double.MAX_VALUE, bestFCost = Double.MAX_VALUE;
        System.out.println("\n[ramp placement sweep on TRAIN seeds] (lo,hi) -> analytic cost | fuzzy cost");
        for (final int[] r : ramps) {
            final double ca = avgCostTrain(() -> ActivationPolicy.analytic(r[0], r[1]));
            final double cf = avgCostTrain(() -> ActivationPolicy.fuzzy(r[0], r[1]));
            System.out.printf("  (%d,%d) -> analytic %.1f | fuzzy %.1f%n", r[0], r[1], ca, cf);
            if (ca < bestACost) { bestACost = ca; bestA = r; }
            if (cf < bestFCost) { bestFCost = cf; bestF = r; }
        }
        final int[] ra = bestA, rf = bestF;
        System.out.printf("  selected on TRAIN: analytic(%d,%d), fuzzy(%d,%d)%n", ra[0], ra[1], rf[0], rf[1]);

        // ---- evaluate all policies on TEST seeds ----
        record Pol(String name, java.util.function.Supplier<ActivationPolicy> p) {}
        final List<Pol> policies = List.of(
            new Pol("always-on", ActivationPolicy::alwaysOn),
            new Pol("FCFS(theta=inf)", () -> ActivationPolicy.fixed(INF)),
            new Pol("fixed(theta=15)", () -> ActivationPolicy.fixed(15)),
            new Pol("best-fixed(theta=" + thetaName(bt) + ")", () -> ActivationPolicy.fixed(bt)),
            new Pol("analytic(" + ra[0] + "," + ra[1] + ")", () -> ActivationPolicy.analytic(ra[0], ra[1])),
            new Pol("fuzzy(" + rf[0] + "," + rf[1] + ")", () -> ActivationPolicy.fuzzy(rf[0], rf[1])),
            new Pol("learned(lookup)", () -> ActivationPolicy.learned(table, BUCKET_W))
        );

        System.out.printf("%n(TEST seeds %d..%d, mean+/-CI95)%n", TRAIN, TRAIN + TEST - 1);
        System.out.printf("%-22s %12s %12s %13s %10s %11s %10s%n",
            "policy", "resp.p0", "resp.p9", "waitMAX.p9", "starv.p9", "overhead.ms", "cost");
        for (final Pol pol : policies) {
            final var p0 = new ArrayList<Double>(); final var p9 = new ArrayList<Double>();
            final var wmax = new ArrayList<Double>(); final var st = new ArrayList<Double>();
            final var ov = new ArrayList<Double>(); final var co = new ArrayList<Double>();
            for (long seed = TRAIN; seed < TRAIN + TEST; seed++) {
                final var r = run(pol.p().get(), phased(seed));
                final var f = r.finished();
                p0.add(Metrics.meanResponseOfClass(f, 0));
                p9.add(Metrics.meanResponseOfClass(f, 9));
                wmax.add(Metrics.maxWaitOfClass(f, 9));
                st.add((double) Metrics.starvationCount(f, 9, STARV_THRESHOLD));
                ov.add(r.wallMs());
                co.add(cost(f));
            }
            System.out.printf("%-22s %12s %12s %13s %10s %11s %10s%n", pol.name(),
                ci(p0), ci(p9), ci(wmax), ci(st), ci(ov), ci(co));
        }
        System.out.println("\n  (always-on penaliza la calma; fixed mal-ajusta una fase; analytic/fuzzy/learned");
        System.out.println("   adaptan por fase -> menor coste sin entrenar (analytic/fuzzy) o con lookup barato)");
        System.out.println("\n#  FIN EXP-F.");
        System.out.flush();
        System.exit(0);
    }

    // ---- offline learning: coordinate descent over backlog buckets on TRAIN seeds ----
    private static double[] learnTable() {
        final double[] table = new double[N_BUCKETS];
        Arrays.fill(table, 0.5);
        final double[] cand = {0.0, 0.25, 0.5, 0.75, 1.0};
        for (int pass = 0; pass < 2; pass++) {
            for (int b = 0; b < N_BUCKETS; b++) {
                double best = table[b], bestCost = Double.MAX_VALUE;
                for (final double a : cand) {
                    table[b] = a;
                    double c = 0;
                    for (long seed = 0; seed < TRAIN; seed++)
                        c += cost(run(ActivationPolicy.learned(table, BUCKET_W), phased(seed)).finished());
                    c /= TRAIN;
                    if (c < bestCost) { bestCost = c; best = a; }
                }
                table[b] = best;
            }
        }
        return table;
    }

    private static double avgCostTest(final java.util.function.Supplier<ActivationPolicy> p) {
        double c = 0;
        for (long seed = TRAIN; seed < TRAIN + TEST; seed++)
            c += cost(run(p.get(), phased(seed)).finished());
        return c / TEST;
    }

    private static double avgCostTrain(final java.util.function.Supplier<ActivationPolicy> p) {
        double c = 0;
        for (long seed = 0; seed < TRAIN; seed++)
            c += cost(run(p.get(), phased(seed)).finished());
        return c / TRAIN;
    }

    private static String thetaName(final int th) {
        return th == Integer.MAX_VALUE ? "inf(FCFS)" : String.valueOf(th);
    }

    private static double cost(final List<Cloudlet> f) {
        return Metrics.meanResponseOfClass(f, 0) + 10.0 * Metrics.starvationCount(f, 9, STARV_THRESHOLD);
    }

    // ---- non-stationary workload: calm (low rate) -> storm (high rate, mostly p0) -> calm ----
    private static List<Workload.Spec> phased(final long seed) {
        final var c1 = Workload.seeded(seed, 40, 0.4, new int[]{0, 9}, new int[]{1, 1}, BASE_LEN, 0.3, 0.2);
        final var st = Workload.seeded(seed + 1000, 140, 2.0, new int[]{0, 9}, new int[]{5, 1}, BASE_LEN, 0.3, 0.2);
        final var c2 = Workload.seeded(seed + 2000, 40, 0.4, new int[]{0, 9}, new int[]{1, 1}, BASE_LEN, 0.3, 0.2);
        final double off1 = lastArrival(c1) + 5;
        final double off2 = off1 + lastArrival(st) + 5;
        final var all = new ArrayList<Workload.Spec>(c1.size() + st.size() + c2.size());
        all.addAll(c1);
        for (final var s : st) all.add(new Workload.Spec(s.arrival() + off1, s.priority(), s.length()));
        for (final var s : c2) all.add(new Workload.Spec(s.arrival() + off2, s.priority(), s.length()));
        return all;
    }

    private static double lastArrival(final List<Workload.Spec> s) {
        double m = 0;
        for (final var x : s) m = Math.max(m, x.arrival());
        return m;
    }

    private static double[] round(final double[] a) {
        final double[] r = new double[a.length];
        for (int i = 0; i < a.length; i++) r[i] = Math.round(a[i] * 100) / 100.0;
        return r;
    }

    private static String ci(final List<Double> v) {
        final double[] mc = Metrics.meanCi95(v);
        return String.format("%.1f±%.1f", mc[0], mc[1]);
    }

    private static Result run(final ActivationPolicy policy, final List<Workload.Spec> specs) {
        final var sim = new CloudSimPlus();
        final var peList = new ArrayList<Pe>(VM_PES);
        for (int i = 0; i < VM_PES; i++) peList.add(new PeSimple(MIPS));
        final Host host = new HostSimple(8192, 100_000, 1_000_000, peList);
        new DatacenterSimple(sim, List.of(host));
        final var broker = new DatacenterBrokerSimple(sim);
        broker.setVmDestructionDelay(1_000_000.0);
        final var vm = new VmPatched(MIPS, VM_PES);
        vm.setRam(4096).setBw(10_000).setSize(10_000).setCloudletScheduler(
            new CloudletSchedulerColaPri().setPriorityWeight(0, W_HIGH).setPriorityWeight(9, W_LOW)
                .setActivationPolicy(policy));
        broker.submitVmList(List.of(vm));
        final var cloudlets = new ArrayList<Cloudlet>(specs.size());
        for (final var s : specs) {
            final var c = Workload.cloudlet(s.length());
            c.setSizes(1024);
            c.setPriority(s.priority());
            c.setSubmissionDelay(s.arrival());
            cloudlets.add(c);
        }
        broker.submitCloudletList(cloudlets);
        final long t0 = System.nanoTime();
        sim.start();
        return new Result(Metrics.checked(broker.getCloudletFinishedList(), cloudlets.size()), (System.nanoTime() - t0) / 1e6);
    }
}
