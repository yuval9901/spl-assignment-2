package bgu.spl.mics.application.messages;

import java.util.List;

import bgu.spl.mics.Broadcast;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.TrackedObject;

public class LastFrameBroadcast implements Broadcast
{
    private StampedDetectedObjects camera_last_frame;
    private List<TrackedObject> lidar_last_frame;
    private String sender;

    public LastFrameBroadcast(StampedDetectedObjects lastFrame,String sender)
    {
        this.camera_last_frame = lastFrame;
        this.lidar_last_frame = null;
        this.sender = sender;
    }

    public LastFrameBroadcast(List<TrackedObject> lastFrame,String sender)
    {
        this.camera_last_frame = null;
        this.lidar_last_frame = lastFrame;
        this.sender = sender;
    }

    public StampedDetectedObjects getCameraLastFrame()
    {
        return this.camera_last_frame;
    }

    public List<TrackedObject> getLidarLastFrame()
    {
        return this.lidar_last_frame;
    }

    public String getSender()
    {
        return this.sender;
    }
}
