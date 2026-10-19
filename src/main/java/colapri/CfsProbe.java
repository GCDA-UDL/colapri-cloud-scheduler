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
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerCompletelyFair;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;

/**
 * Gating probe: does CloudletSchedulerCompletelyFair (CFS, the built-in fair baseline reviewers ask
 * for) actually terminate with a backlog if we set a Datacenter schedulingInterval? Earlier it hung.
 * If this prints a finished count, CFS is usable as a baseline; otherwise we implement a clean WFQ.
 */
public class CfsProbe {
    public static void main(String[] args) {
        org.cloudsimplus.util.Log.setLevel(ch.qos.logback.classic.Level.OFF);
        final long t0 = System.currentTimeMillis();
        System.out.println("[CfsProbe] start");
        System.out.flush();

        final var sim = new CloudSimPlus();
        final var peList = new ArrayList<Pe>(2);
        for (int i = 0; i < 2; i++) peList.add(new PeSimple(1000));
        final Host host = new HostSimple(8192, 100_000, 1_000_000, peList);
        final var dc = new DatacenterSimple(sim, List.of(host));
        dc.setSchedulingInterval(1.0);   // <-- the fix being tested: periodic updates instead of event storm

        final var broker = new DatacenterBrokerSimple(sim);
        broker.setVmDestructionDelay(100_000.0);
        final var vm = new VmSimple(1000, 2);
        vm.setRam(4096).setBw(10_000).setSize(10_000)
          .setCloudletScheduler(new CloudletSchedulerCompletelyFair());
        broker.submitVmList(List.of(vm));

        final var cloudlets = new ArrayList<Cloudlet>(40);
        for (int i = 0; i < 40; i++) {
            final var c = new CloudletSimple(4000, 1, new UtilizationModelFull());
            c.setSizes(1024);
            c.setPriority(i % 3 == 0 ? 0 : (i % 3 == 1 ? 5 : 9));
            c.setSubmissionDelay(i * 0.2);   // staggered -> backlog
            cloudlets.add(c);
        }
        broker.submitCloudletList(cloudlets);

        sim.start();
        final var fin = broker.getCloudletFinishedList();
        System.out.printf("[CfsProbe] DONE: %d/40 finished, makespan=%.1f, wall=%dms%n",
            fin.size(), Metrics.makespan(fin), System.currentTimeMillis() - t0);
        System.out.flush();
        System.exit(0);
    }
}
