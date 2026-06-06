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
import org.cloudsimplus.schedulers.cloudlet.CloudletScheduler;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerCompletelyFair;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * EXP-A: rigorous (multi-seed) scheduling evaluation on synthetic workload W1.
 *
 * Upgrades Experiments.java to journal grade: N_SEEDS independent workload instances per config,
 * every metric reported as mean +/- CI95, scheduler decision OVERHEAD measured (wall time of
 * sim.start()), weighted-Jain aligned to the scheduler's class weights. Baselines: FCFS, SJF,
 * StrictPriority, Priority+Aging, (optional CFS), ColaPri.
 *
 * Three scenarios: MAIN comparison, STARVATION (isolated), ADAPTIVE (load-gated activation).
 */
public class RigorousEval {
    private static final int MIPS = 1000;
    private static final int VM_PES = 2;
    private static final long BASE_LEN = 4000;
    private static final int N_SEEDS = 30;
    private static final double STARV_THRESHOLD = 30.0;   // wait > 30s counts as starved (class 9)

    /** Set true once CfsProbe confirms CFS terminates with a Datacenter schedulingInterval. */
    private static final boolean INCLUDE_CFS = false;

    private record Result(List<Cloudlet> finished, double wallMs) {}

    public static void main(String[] args) {
        org.cloudsimplus.util.Log.setLevel(ch.qos.logback.classic.Level.OFF);
        // align the fairness metric to the weights ColaPri actually uses (6:3:1 for classes 0/5/9)
        Metrics.setClassWeights(Map.of(0, 6, 5, 3, 9, 1));

        System.out.println("##########################################################################");
        System.out.println("#  EXP-A · ColaPri rigorous scheduling eval (W1 synthetic, " + N_SEEDS + " seeds, CI95)");
        System.out.println("#  overhead = wall time of sim.start() (proxy for scheduling decision cost)");
        System.out.println("##########################################################################");

        scenarioMain();
        scenarioStarvation();
        scenarioAdaptive();

        System.out.println("\n#  FIN EXP-A.");
        System.out.flush();
        System.exit(0);
    }

    // ---- MAIN: moderate-high load, 3 classes, weighted differentiation + fairness + overhead ----
    private static void scenarioMain() {
        System.out.println("\n=== MAIN · 60 tareas, ráfaga 5/s, clases 0/5/9 (mix 3:2:1), 30 seeds ===");
        final var schedulers = schedulers();
        // metric samples per scheduler
        final Map<String, List<Double>> respHigh = init(schedulers), p95High = init(schedulers),
            respMid = init(schedulers), respLow = init(schedulers), waitMaxLow = init(schedulers),
            starvLow = init(schedulers), jain = init(schedulers), makespan = init(schedulers), overhead = init(schedulers);

        for (long seed = 0; seed < N_SEEDS; seed++) {
            final var w = Workload.seeded(seed, 60, 5.0, new int[]{0, 5, 9}, new int[]{3, 2, 1}, BASE_LEN, 0.3, 0.2);
            for (final var e : schedulers.entrySet()) {
                final var r = run(e.getValue(), w, e.getKey().equals("CFS"));
                final var f = r.finished();
                respHigh.get(e.getKey()).add(Metrics.meanResponseOfClass(f, 0));
                p95High.get(e.getKey()).add(Metrics.p95ResponseOfClass(f, 0));
                respMid.get(e.getKey()).add(Metrics.meanResponseOfClass(f, 5));
                respLow.get(e.getKey()).add(Metrics.meanResponseOfClass(f, 9));
                waitMaxLow.get(e.getKey()).add(Metrics.maxWaitOfClass(f, 9));
                starvLow.get(e.getKey()).add((double) Metrics.starvationCount(f, 9, STARV_THRESHOLD));
                jain.get(e.getKey()).add(Metrics.jainWeighted(f));
                makespan.get(e.getKey()).add(Metrics.makespan(f));
                overhead.get(e.getKey()).add(r.wallMs());
            }
        }
        System.out.printf("%n%-16s %14s %14s %14s %12s %10s %9s %11s%n",
            "scheduler", "resp.p0", "resp.p5", "resp.p9", "waitMAX.p9", "starv.p9", "Jain", "overhead.ms");
        for (final String k : schedulers.keySet())
            System.out.printf("%-16s %14s %14s %14s %12s %10s %9s %11s%n", k,
                ci(respHigh.get(k)), ci(respMid.get(k)), ci(respLow.get(k)), ci(waitMaxLow.get(k)),
                ci(starvLow.get(k)), ci(jain.get(k)), ci(overhead.get(k)));
        System.out.println("  (resp por clase: FCFS≈plano · Strict≈extremo · ColaPri≈graduado p0<p5<p9 · Jain alto=mejor)");
    }

    // ---- STARVATION isolated: continuous high-priority arrivals, NOT saturated ----
    private static void scenarioStarvation() {
        System.out.println("\n\n=== STARVATION (aislada, llegadas SOSTENIDAS) · 200 tareas 90% p0 + 10% p9, 0.6/s, 30 seeds ===");
        System.out.println("  (rate≈capacidad → p0 fresco llega mientras p9 viejo espera; foco: espera MÁX / inanidos p9)");
        final var schedulers = schedulers();
        final Map<String, List<Double>> waitMaxLow = init(schedulers), starvLow = init(schedulers), respHigh = init(schedulers);
        for (long seed = 0; seed < N_SEEDS; seed++) {
            final var w = Workload.seeded(seed, 200, 0.6, new int[]{0, 9}, new int[]{9, 1}, BASE_LEN, 0.3, 0.2);
            for (final var e : schedulers.entrySet()) {
                final var f = run(e.getValue(), w, e.getKey().equals("CFS")).finished();
                waitMaxLow.get(e.getKey()).add(Metrics.maxWaitOfClass(f, 9));
                starvLow.get(e.getKey()).add((double) Metrics.starvationCount(f, 9, STARV_THRESHOLD));
                respHigh.get(e.getKey()).add(Metrics.meanResponseOfClass(f, 0));
            }
        }
        System.out.printf("%n%-16s %14s %12s %14s%n", "scheduler", "waitMAX.p9", "starv.p9", "resp.p0(coste)");
        for (final String k : schedulers.keySet())
            System.out.printf("%-16s %14s %12s %14s%n", k, ci(waitMaxLow.get(k)), ci(starvLow.get(k)), ci(respHigh.get(k)));
        System.out.println("  (Strict: waitMAX.p9 explota · ColaPri: acotada, manteniendo resp.p0 baja)");
    }

    // ---- ADAPTIVE: below threshold ColaPri == FCFS; above it differentiates ----
    private static void scenarioAdaptive() {
        System.out.println("\n\n=== ADAPTIVE (umbral=15) · carga BAJA vs ALTA, 30 seeds ===");
        final Supplier<CloudletScheduler> adapt =
            () -> new CloudletSchedulerColaPri().setPriorityWeight(0, 6).setPriorityWeight(9, 1).setAdaptiveThreshold(15);
        final Supplier<CloudletScheduler> fcfs = CloudletSchedulerSpaceShared::new;
        final var loLevels = new String[]{"BAJA (sin backlog)", "ALTA (ráfaga, backlog)"};
        final double[] rates = {0.3, 3.0};
        final int[] ns = {24, 80};
        for (int s = 0; s < 2; s++) {
            final var dH_f = new ArrayList<Double>(); final var dH_c = new ArrayList<Double>();
            final var dL_f = new ArrayList<Double>(); final var dL_c = new ArrayList<Double>();
            for (long seed = 0; seed < N_SEEDS; seed++) {
                final var w = Workload.seeded(seed, ns[s], rates[s], new int[]{0, 9}, new int[]{1, 1}, BASE_LEN, 0.3, 0.2);
                final var ff = run(fcfs, w, false).finished();
                final var cf = run(adapt, w, false).finished();
                dH_f.add(Metrics.meanResponseOfClass(ff, 0)); dL_f.add(Metrics.meanResponseOfClass(ff, 9));
                dH_c.add(Metrics.meanResponseOfClass(cf, 0)); dL_c.add(Metrics.meanResponseOfClass(cf, 9));
            }
            System.out.printf("%n  carga %s:%n", loLevels[s]);
            System.out.printf("    %-16s resp.p0=%-14s resp.p9=%-14s%n", "FCFS", ci(dH_f), ci(dL_f));
            System.out.printf("    %-16s resp.p0=%-14s resp.p9=%-14s%n", "ColaPri(adapt)", ci(dH_c), ci(dL_c));
        }
        System.out.println("  (BAJA: ColaPri≈FCFS, p0≈p9 → no diferencia, coste cero · ALTA: p0<p9 → se activa)");
    }

    // ---- infra ----
    private static Map<String, Supplier<CloudletScheduler>> schedulers() {
        final Map<String, Supplier<CloudletScheduler>> m = new LinkedHashMap<>();
        m.put("FCFS", CloudletSchedulerSpaceShared::new);
        m.put("SJF", CloudletSchedulerSJF::new);
        m.put("StrictPriority", CloudletSchedulerStrictPriority::new);
        m.put("Priority+Aging", () -> new CloudletSchedulerPriorityAging(0.2));  // tuned so aging actually fires (0.05 degenerates to strict)
        m.put("WFQ-fair", CloudletSchedulerWFQ::new);   // fair-share baseline (CFS replacement; CFS hangs on backlog)
        if (INCLUDE_CFS) m.put("CFS", CloudletSchedulerCompletelyFair::new);
        m.put("ColaPri", () -> new CloudletSchedulerColaPri()
            .setPriorityWeight(0, 6).setPriorityWeight(5, 3).setPriorityWeight(9, 1));
        return m;
    }

    private static Map<String, List<Double>> init(final Map<String, ?> schedulers) {
        final Map<String, List<Double>> m = new LinkedHashMap<>();
        for (final String k : schedulers.keySet()) m.put(k, new ArrayList<>());
        return m;
    }

    private static String ci(final List<Double> v) {
        final double[] mc = Metrics.meanCi95(v);
        return String.format("%.1f±%.1f", mc[0], mc[1]);
    }

    private static Result run(final Supplier<CloudletScheduler> factory, final List<Workload.Spec> specs, final boolean schedInterval) {
        final var sim = new CloudSimPlus();
        final var peList = new ArrayList<Pe>(VM_PES);
        for (int i = 0; i < VM_PES; i++) peList.add(new PeSimple(MIPS));
        final Host host = new HostSimple(8192, 100_000, 1_000_000, peList);
        final var dc = new DatacenterSimple(sim, List.of(host));
        if (schedInterval) dc.setSchedulingInterval(1.0);   // needed for CFS to terminate
        final var broker = new DatacenterBrokerSimple(sim);
        broker.setVmDestructionDelay(100_000.0);
        final var vm = new VmSimple(MIPS, VM_PES);
        vm.setRam(4096).setBw(10_000).setSize(10_000).setCloudletScheduler(factory.get());
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
        final double wallMs = (System.nanoTime() - t0) / 1e6;
        return new Result(broker.getCloudletFinishedList(), wallMs);
    }
}
