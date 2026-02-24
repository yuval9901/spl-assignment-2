package bgu.spl.mics.application.services;

import java.util.concurrent.CountDownLatch;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.EndTimeBroadcast;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * TimeService acts as the global timer for the system, broadcasting TickBroadcast messages
 * at regular intervals and controlling the simulation's duration.
 */
public class TimeService extends MicroService 
{
    private int currentTick;
    private int duration;
    private int tickTime;

    /**
     * Constructor for TimeService.
     *
     * @param TickTime  The duration of each tick in milliseconds.
     * @param Duration  The total number of ticks before the service terminates.
     */
    public TimeService(int TickTime, int Duration) 
    {
        super("TimeService");
        this.duration = Duration;
        this.tickTime = TickTime;
        this.currentTick = 0;
    }

    /**
     * Initializes the TimeService.
     * Starts broadcasting TickBroadcast messages and terminates after the specified duration.
     */
    @Override
    protected void initialize() 
    {
        subscribeBroadcast(EndTimeBroadcast.class, event -> {this.terminate();});
        // Subscribe to TerminatedBroadcast to terminate the service
        subscribeBroadcast(TerminatedBroadcast.class, broadcast -> this.terminate());

        // Subscribe to CrashedBroadcast to terminate the service
        subscribeBroadcast(CrashedBroadcast.class, broadcast -> terminate());
        // Run the tick broadcasting in the same thread as the MicroService

        subscribeBroadcast(TickBroadcast.class, tick -> {
            try
            {
                if(tick.getCurrentTick() < duration)
                {
                    Thread.sleep(tickTime);
                    currentTick++;
                    System.out.println("Current Tick: " + currentTick);
                    StatisticalFolder.getInstance().incrementSystemRuntime();
                    this.sendBroadcast(new TickBroadcast(currentTick));
                }
                else
                {
                    sendBroadcast(new TerminatedBroadcast(this.getName()));
                }
            }
            catch(InterruptedException ex)
            {
                System.err.println(ex);
            }
        });
        this.sendBroadcast(new TickBroadcast(currentTick));
    }
}
