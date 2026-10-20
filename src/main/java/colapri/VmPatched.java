package colapri;

import org.cloudsimplus.schedulers.MipsShare;
import org.cloudsimplus.vms.VmSimple;

/**
 * {@link VmSimple} with a workaround for an event-scheduling defect of CloudSim Plus 8.5.7.
 *
 * <p>{@code VmAbstract.cloudletsProcessing()} subtracts the fractional part of the current clock from the
 * next-event delay returned by the cloudlet scheduler ("decimals" adjustment). When the two are equal the
 * adjusted delay is exactly 0; {@code HostAbstract.updateVmProcessing()} then discards it ({@code delay > 0}),
 * the host reports {@code Double.MAX_VALUE}, the datacenter never schedules the next processing update and
 * the simulation ends with cloudlets still running or queued (observed in the 2026-10-20 audit: EXP-D
 * theta=60 seed 15 lost 54/80 cloudlets; EXP-F seed 22 stalled until the VM destruction delay).</p>
 *
 * <p>The workaround returns the simulation's minimum inter-event time instead of 0 whenever cloudlets
 * remain, which keeps the event chain alive. Runs in which the adjusted delay is not 0 are unaffected.</p>
 */
public class VmPatched extends VmSimple {

    public VmPatched(final double mipsCapacity, final long pesNumber) {
        super(mipsCapacity, pesNumber);
    }

    @Override
    public double updateProcessing(final double currentTime, final MipsShare mipsShare) {
        final double delay = super.updateProcessing(currentTime, mipsShare);
        if (delay == 0 && !getCloudletScheduler().isEmpty()) {
            return getSimulation().getMinTimeBetweenEvents();
        }
        return delay;
    }
}
