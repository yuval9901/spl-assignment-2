package bgu.spl.mics.application.services;

import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.StampedCloudPoints;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.StatisticalFolder;
import bgu.spl.mics.application.objects.TrackedObject;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.LastFrameBroadcast;
import bgu.spl.mics.application.messages.MicroServiceChangeStateBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.LiDarDataBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LiDarService is responsible for processing data from the LiDAR sensor and
 * sending TrackedObjectsEvents to the FusionSLAM service.
 * 
 * This service interacts with the LiDarWorkerTracker object to retrieve and process
 * cloud point data and updates the system's map upon sending its observations.
 */
public class LiDarService extends MicroService {

    private final LiDarWorkerTracker workerTracker;
    private final LiDarDataBase liDarDataBase;
    private HashMap<Integer,List<TrackedObject>> pendingObjects;
    private AtomicInteger currentTick = new AtomicInteger();
    private boolean isCamerasFinished;
    private List<TrackedObject> lastFrame = new ArrayList<>();
    private CountDownLatch latch;

    public LiDarService(LiDarWorkerTracker workerTracker,CountDownLatch latch) {
        super("LiDarService-" + workerTracker.getId());
        this.workerTracker = workerTracker;
        this.liDarDataBase = LiDarDataBase.getInstance();
        this.pendingObjects = new HashMap<>();
        this.currentTick.set(0);
        this.isCamerasFinished = false;
        this.latch = latch;
    }

    @Override
    protected void initialize() {
        subscribeBroadcast(MicroServiceChangeStateBroadcast.class,event ->{
            if(event.getService() instanceof CameraService)
            {
                this.isCamerasFinished = true;
            }
        });
        // Handle DetectObjectsEvent immediately
        subscribeEvent(DetectObjectsEvent.class, event -> {
            StampedDetectedObjects detectedObjects = event.getDetectedObjects();
            int detectionTime = detectedObjects.getTime();

            for(StampedCloudPoints cloudPoints : liDarDataBase.getCloudPoints(detectionTime))
            {
                if(cloudPoints.getId().equals("ERROR"))
                {
                    workerTracker.setStatus(STATUS.ERROR);
                    this.sendBroadcast(new CrashedBroadcast(getName(), detectionTime, "Lidar sensor disconnected"));
                    break;
                }
            }

            // Process the objects if the worker is active and the tick condition is met
            if (workerTracker.getStatus() == STATUS.UP)
            {
                List<TrackedObject> trackedObjects = new ArrayList<>();
                for (DetectedObject detectedObject : detectedObjects.getDetectedObjects()) {
                    for (StampedCloudPoints cloudPoints : liDarDataBase.getCloudPoints(detectionTime)) {
                        if (cloudPoints.getId().equals(detectedObject.getId()) && cloudPoints.getTime() == detectionTime) 
                        {
                            TrackedObject trackedObject = new TrackedObject(detectedObject.getId(), detectedObject.getDescription(), detectionTime, cloudPoints.getCoordinates());
                            trackedObjects.add(trackedObject);
                            StatisticalFolder.getInstance().incrementTrackedObjects(1);
                            workerTracker.getLastTrackedObjects().add(trackedObject);
                        }
                    }
                }
                if(currentTick.get() >= detectionTime + workerTracker.getFrequency())
                {
                    if(this.workerTracker.getStatus() == STATUS.UP)
                    {
                        this.sendEvent(new TrackedObjectsEvent(trackedObjects));
                        this.lastFrame = trackedObjects;
                        for(TrackedObject trackedObject : trackedObjects)
                        {
                            System.out.println("Lidar sent "+ trackedObject.getId()+" to fusion slam.");
                        }
                    }
                }
                else
                {
                    if(!pendingObjects.containsKey(detectionTime+this.workerTracker.getFrequency()))
                    {
                        pendingObjects.put(detectionTime+this.workerTracker.getFrequency(), trackedObjects);
                    }
                    else
                    {
                        pendingObjects.get(detectionTime+this.workerTracker.getFrequency()).addAll(trackedObjects);
                    }
                }
            }
        });

        // Handle TickBroadcast - Check pending objects on each tick
        subscribeBroadcast(TickBroadcast.class, tick -> {
            if(!(this.isCamerasFinished && MessageBusImpl.getInstance().isRegistered(CameraService.class,this) && !this.pendingObjects.containsKey(this.currentTick.get())))
            {
                this.currentTick.set(tick.getCurrentTick());
                if(this.pendingObjects.containsKey(currentTick.get()))
                {
                    List<TrackedObject> penList = this.pendingObjects.get(this.currentTick.get());
                    this.sendEvent(new TrackedObjectsEvent(penList));
                    lastFrame.clear();
                    for(TrackedObject obj : penList)
                    {
                        lastFrame.add(obj);
                    }
                    this.pendingObjects.get(this.currentTick.get()).removeAll(penList);
                }
            }
            else
            {
                this.terminate();
            }
        });

        // Subscribe to TerminatedBroadcast to terminate the service
        subscribeBroadcast(TerminatedBroadcast.class, broadcast -> this.terminate());

        // Subscribe to CrashedBroadcast to terminate the service
        subscribeBroadcast(CrashedBroadcast.class, broadcast -> {
            this.sendBroadcast(new LastFrameBroadcast(this.lastFrame,"LiDarWorkerTracker"+this.workerTracker.getId()));
            terminate();
        });
        latch.countDown();
        System.out.println(this.getName() + " finished init");
    }
}
