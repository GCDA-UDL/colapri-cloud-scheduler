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

    // persistent ColaPri state (survives across scheduling events)
    private final Map<Long, Double> tag = new HashMap<>();           // cloudlet id -> virtual tag
    private final Map<Integer, Integer> seen = new HashMap<>();      // per-class arrival counter

    public CloudletSchedulerColaPri setPriorityWeight(final int priority, final int weight) {
        weights.put(clamp(priority), weight);
        return this;
    }

    public CloudletSchedulerColaPri setAdaptiveThreshold(final int threshold) {
        this.adaptiveThreshold = Math.max(0, threshold);
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
        return super.moveNextCloudletsFromWaitingToExecList(currentTime);
    }

    private void orderWaitingByColaPri() {
        final List<CloudletExecution> waiting = getCloudletWaitingList();
        // adaptive: below the load threshold, stay plain FIFO (no priority reordering)
        if (waiting.size() < 2 || waiting.size() <= adaptiveThreshold) {
            return;
        }
        // assign a persistent weighted-round-robin tag to any cloudlet not seen yet
        for (final CloudletExecution cle : waiting) {
            tag.computeIfAbsent(cle.getId(), id -> {
                final int c = clamp(cle.getCloudlet().getPriority());
                final int k = seen.merge(c, 1, Integer::sum) - 1;    // 0-based index within class
                return (double) k / weight(c);                      // ColaPri weighted-RR virtual tag
            });
        }
        sortCloudletWaitingList(
            Comparator.comparingDouble(cle -> tag.getOrDefault(cle.getId(), Double.MAX_VALUE)));
    }
}
