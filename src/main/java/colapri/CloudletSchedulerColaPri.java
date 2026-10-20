package colapri;

import org.cloudsimplus.cloudlets.CloudletExecution;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CloudletSchedulerColaPri — ColaPri (Class-Oriented Load-Adaptive PRIority) realized as
 * a CloudSim Plus cloudlet scheduler on top of space-shared PE allocation.
 *
 * <p>Dispatches waiting cloudlets in ColaPri's <b>weighted round-robin</b> order by priority
 * class: a class of weight w gets ~w dispatch slots per round, so higher classes go faster
 * WITHOUT starving lower ones (each class keeps interleaved slots proportional to its weight).</p>
 *
 * <p><b>Persistence matters.</b> A scheduler is invoked many times as PEs free up; if the
 * round-robin position were recomputed from scratch each call, only the top class would ever be
 * picked and the policy would collapse into strict priority. ColaPri's weighted round-robin is
 * therefore realized with <em>persistent virtual-time tags</em> (equivalent to weighted
 * round-robin / WFQ virtual clock): the k-th arriving cloudlet of class c gets the tag
 * {@code k / weight(c)}, assigned once; the waiting list is ordered by tag. Higher-weight classes
 * get smaller tag increments → more, earlier slots, interleaved with the lower ones.</p>
 *
 * <p>Weights are configurable ({@code setPriorityWeight}); default per class follows ColaPri's
 * repetition table {60,50,...,1}. Adaptive activation: below {@code adaptiveThreshold} waiting
 * cloudlets it stays plain FIFO (no priority cost).</p>
 */
public class CloudletSchedulerColaPri extends CloudletSchedulerSpaceShared {

    private static final int[] DEFAULT_REP = {60, 50, 40, 30, 15, 10, 5, 3, 2, 1};

    private final Map<Integer, Integer> weights = new HashMap<>();   // class -> weight (override)
    private int adaptiveThreshold = 0;
    private boolean resync = true;                // resynchronise an idle class's counter to the frontier (ablation: false)
    private ActivationPolicy activation = null;   // EXP-F: auto-tuned activation (fuzzy/analytic/learned)
    private int prevBacklog = 0;                  // for growth-aware activation policies

    // persistent ColaPri state (survives across scheduling events)
    private final Map<Long, Double> tag = new HashMap<>();           // cloudlet id -> virtual tag
    private final Map<Integer, Integer> seen = new HashMap<>();      // per-class arrival counter
    private double virtualTime = 0.0;                                // frontier: largest tag already dispatched

    public CloudletSchedulerColaPri setPriorityWeight(final int priority, final int weight) {
        weights.put(clamp(priority), weight);
        return this;
    }

    public CloudletSchedulerColaPri setAdaptiveThreshold(final int threshold) {
        this.adaptiveThreshold = Math.max(0, threshold);
        return this;
    }

    /** Ablation switch: false restores the original rule without counter resynchronisation. */
    public CloudletSchedulerColaPri setResync(final boolean resync) {
        this.resync = resync;
        return this;
    }

    /** EXP-F: replace the fixed threshold with an automatic activation policy returning an
     *  activation degree alpha in [0,1] (0 = FIFO, 1 = full weighted-RR). The effective per-class
     *  weight blends toward neutral as alpha -> 0: w_eff = 1 + alpha*(w-1). */
    public CloudletSchedulerColaPri setActivationPolicy(final ActivationPolicy policy) {
        this.activation = policy;
        return this;
    }

    private int weight(final int priority) {
        final int c = clamp(priority);
        final Integer w = weights.get(c);
        return (w != null && w > 0) ? w : DEFAULT_REP[c];
    }

    private int clamp(final int p) {
        return p < 0 ? 0 : (p > 9 ? 9 : p);
    }

    @Override
    protected double moveNextCloudletsFromWaitingToExecList(final double currentTime) {
        orderWaitingByColaPri();
        final double next = super.moveNextCloudletsFromWaitingToExecList(currentTime);
        // advance the frontier to the largest tag now in service (SFQ-style virtual time)
        for (final CloudletExecution cle : getCloudletExecList()) {
            final Double t = tag.get(cle.getId());
            if (t != null && t > virtualTime) virtualTime = t;
        }
        return next;
    }

    private void orderWaitingByColaPri() {
        final List<CloudletExecution> waiting = getCloudletWaitingList();
        if (waiting.size() < 2) return;
        final int backlog = waiting.size();

        // activation degree alpha in [0,1]: 0 = stay FIFO, 1 = full weighted-RR
        final double a;
        if (activation != null) {
            a = activation.alpha(backlog, prevBacklog);
            prevBacklog = backlog;
        } else {
            prevBacklog = backlog;
            if (backlog <= adaptiveThreshold) return;   // fixed-threshold mode (legacy)
            a = 1.0;
        }
        if (a <= 0.0) return;
        final double alpha = a;

        // assign a persistent weighted-round-robin tag to any cloudlet not seen yet;
        // effective weight blends toward neutral (1) as alpha shrinks.
        // Resynchronisation: a class returning from idleness may not mint tags below the frontier
        // (the largest tag already dispatched), so its counter jumps to ceil(frontier * w) first.
        for (final CloudletExecution cle : waiting) {
            tag.computeIfAbsent(cle.getId(), id -> {
                final int c = clamp(cle.getCloudlet().getPriority());
                final double wEff = 1.0 + alpha * (weight(c) - 1.0); // alpha=1 -> w, alpha=0 -> 1
                final int kMin = resync ? (int) Math.ceil(virtualTime * wEff - 1e-9) : 0;
                final int k = Math.max(seen.getOrDefault(c, 0), kMin); // 0-based index within class
                seen.put(c, k + 1);
                return k / wEff;                                     // ColaPri weighted-RR virtual tag
            });
        }
        sortCloudletWaitingList(
            Comparator.comparingDouble(cle -> tag.getOrDefault(cle.getId(), Double.MAX_VALUE)));
    }
}
