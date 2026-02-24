package bgu.spl.mics.application.objects;

public class CameraLastFrame 
{
    private StampedDetectedObjects last_objects_detected;
    private String sender_name;

    public CameraLastFrame(StampedDetectedObjects objects, String name)
    {
        this.last_objects_detected = objects;
        this.sender_name = name;
    }

    public StampedDetectedObjects getDetectedObjects()
    {
        return this.last_objects_detected;
    }

    public String getSenderName()
    {
        return this.sender_name;
    }
}
