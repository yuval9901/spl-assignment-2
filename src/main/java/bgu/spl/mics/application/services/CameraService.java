package bgu.spl.mics.application.services;

import java.util.concurrent.CountDownLatch;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.LastFrameBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.StatisticalFolder;

public class CameraService extends MicroService {

    private final Camera camera;
    private int numOfDetectedObject;
    private StampedDetectedObjects lastFrame;
    private CountDownLatch latch;

    public CameraService(Camera camera,CountDownLatch latch) {
        super("CameraService-" + camera.getId());
        this.camera = camera;
        this.numOfDetectedObject = 0;
        this.lastFrame = null;
        this.latch = latch;
    }

    @Override
    protected void initialize() 
    {
        // Subscribe to TickBroadcast
        subscribeBroadcast(TickBroadcast.class, tick -> {
            int currentTick = tick.getCurrentTick();

            // Check if the camera is active
            if (camera.getStatus() == STATUS.UP) {
                for (StampedDetectedObjects obj : camera.getListOfDetectedObjects()) {
                    // Check if the detection aligns with the camera's sending interval
                    if (obj.getTime() + camera.getFrequency() == currentTick) 
                    {
                        for (DetectedObject detectedObject : obj.getDetectedObjects()) 
                        {
                            if(detectedObject.getId().equals("ERROR"))
                            {
                                this.camera.setStatus(STATUS.ERROR);
                                this.sendBroadcast(new CrashedBroadcast(getName(), currentTick,detectedObject.getDescription()));
                                break;
                            }
                            else
                            {
                                StatisticalFolder.getInstance().incrementDetectedObjects(1);
                            }
                        }
                        if(camera.getStatus() == STATUS.UP)
                        {
                            DetectObjectsEvent event = new DetectObjectsEvent(obj, getName());
                            this.lastFrame = obj;
                            sendEvent(event);
                            this.numOfDetectedObject++;
                            if(numOfDetectedObject == camera.getListOfDetectedObjects().size())
                            {
                                this.camera.setStatus(STATUS.DOWN);
                            }
                            for (DetectedObject o : obj.getDetectedObjects()) {
                                System.out.println("Camera: "+this.camera.getCameraKey() +" detected " + o.getId() + " " + o.getDescription());
                            }
                        }
                        if(camera.getStatus()==STATUS.ERROR)
                        {
                            break;
                        }
                    }
                }
            }
            if(camera.getStatus() == STATUS.DOWN)
            {
                this.terminate();
            }
        });

        // Subscribe to TerminatedBroadcast to terminate the service
        subscribeBroadcast(TerminatedBroadcast.class, broadcast -> 
        {
            terminate();
        });

        // Subscribe to CrashedBroadcast to terminate the service
        subscribeBroadcast(CrashedBroadcast.class, broadcast -> {
            sendBroadcast(new LastFrameBroadcast(lastFrame,this.camera.getCameraKey()));
            terminate();
        });
        this.latch.countDown();
        System.out.println(this.getName() + " finished init.");
    }
}
