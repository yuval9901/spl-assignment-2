package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.StampedDetectedObjects;

public class DetectObjectsEvent implements Event<StampedDetectedObjects>
{
    private final StampedDetectedObjects objects;
    private final String senderName;
    

    public DetectObjectsEvent(StampedDetectedObjects objects, String senderName)
    {
        this.objects = objects;
        this.senderName = senderName;
    }

    public String getName()
    {
        return this.senderName;
    }

    public StampedDetectedObjects getDetectedObjects()
    {
        return this.objects;
    }
}
