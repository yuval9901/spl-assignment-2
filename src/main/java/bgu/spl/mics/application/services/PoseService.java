package bgu.spl.mics.application.services;

import java.util.concurrent.CountDownLatch;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.CrashedBroadcast;

/**
 * PoseService is responsible for maintaining the robot's current pose (position and orientation)
 * and broadcasting PoseEvents at every tick.
 */
public class PoseService extends MicroService {

    private final GPSIMU gpsimu;
    private int posesSent;
    private CountDownLatch latch;

    /**
     * Constructor for PoseService.
     *
     * @param gpsimu The GPSIMU object that provides the robot's pose data.
     */
    public PoseService(GPSIMU gpsimu,CountDownLatch latch) {
        super("PoseService");
        this.gpsimu = gpsimu;
        this.posesSent = 0;
        this.latch = latch;
    }

    /**
     * Initializes the PoseService.
     * Subscribes to TickBroadcast and sends PoseEvents at every tick based on the current pose.
     */
    @Override
    protected void initialize() {
        // Subscribe to TickBroadcast
        subscribeBroadcast(TickBroadcast.class, tick -> {
            int currentTick = tick.getCurrentTick();

            // Check if the GPSIMU system is operational
            if (gpsimu.getStatus() == STATUS.UP) {
                // Retrieve the current pose for the current tick
                Pose currentPose = gpsimu.getListOPoses().stream()
                        .filter(pose -> pose.getTime() == currentTick)
                        .findFirst()
                        .orElse(null);

                if (currentPose != null) {
                    // Create and send a PoseEvent
                    PoseEvent poseEvent = new PoseEvent(currentPose, getName());
                    sendEvent(poseEvent);
                    System.out.println("GPSIMU sent the pose");
                    this.posesSent++;
                }
                if(this.posesSent == this.gpsimu.getListOPoses().size())
                {
                    this.gpsimu.setStatus(STATUS.DOWN);
                }
            }
            if(this.gpsimu.getStatus() == STATUS.DOWN)
            {
                this.terminate();
            }
        });
        // Subscribe to TerminatedBroadcast to terminate the service
        subscribeBroadcast(TerminatedBroadcast.class, broadcast -> terminate());

        // Subscribe to CrashedBroadcast to terminate the service
        subscribeBroadcast(CrashedBroadcast.class, broadcast -> terminate());
        this.latch.countDown();
        System.out.println(this.getName() + " finished init");
    }
}
