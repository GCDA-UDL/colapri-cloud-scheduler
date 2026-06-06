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
 *   always-on, fixed-theta, analytic(ramp), fuzzy(Mamdani), learned(offline lookup).
 *
 * The learned table is fitted by coordinate descent on TRAIN seeds and evaluated on disjoint TEST
 * seeds. Objective: low high-class response without starving the low class
 * (cost = resp.p0 + 10 * starv.p9). Shows the cheap analytic/fuzzy controllers match the learned
 * policy and beat the best single fixed threshold, at zero training cost.
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

        // ---- best single fixed-theta (oracle reference over TEST) ----
        int bestTheta = 0; double bestThetaCost = Double.MAX_VALUE;
        for (final int th : new int[]{0, 5, 10, 20, 40}) {
            final double c = avgCostTest(() -> ActivationPolicy.fixed(th));
            if (c < bestThetaCost) { bestThetaCost = c; bestTheta = th; }
        }
        final int bt = bestTheta;

        // ---- evaluate all policies on TEST seeds ----
        record Pol(String name, java.util.function.Supplier<ActivationPolicy> p) {}
        final List<Pol> policies = List.of(
            new Pol("always-on", ActivationPolicy::alwaysOn),
            new Pol("fixed(theta=15)", () -> ActivationPolicy.fixed(15)),
            new Pol("best-fixed(theta=" + bt + ")", () -> ActivationPolicy.fixed(bt)),
            new Pol("analytic(20,50)", () -> ActivationPolicy.analytic(20, 50)),
            new Pol("fuzzy(20,50)", () -> ActivationPolicy.fuzzy(20, 50)),
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
        final var vm = new VmSimple(MIPS, VM_PES);
        vm.setRam(4096).setBw(10_000).setSize(10_000).setCloudletScheduler(
            new CloudletSchedulerColaPri().setPriorityWeight(0, W_HIGH).setPriorityWeight(9, W_LOW)
                .setActivationPolicy(policy));
        broker.submitVmList(List.of(vm));
        final var cloudlets = new ArrayList<Cloudlet>(specs.size());
        for (final var s : specs) {
            final var c = new CloudletSimple(s.length(), 1, new UtilizationModelFull());
            c.setSizes(1024);
            c.setPriority(s.priority());
            c.setSubmissionDelay(s.arrival());
            cloudlets.add(c);
        }
        broker.submitCloudletList(cloudlets);
        final long t0 = System.nanoTime();
        sim.start();
        return new Result(broker.getCloudletFinishedList(), (System.nanoTime() - t0) / 1e6);
    }
}
