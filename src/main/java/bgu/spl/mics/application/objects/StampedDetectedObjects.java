package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents objects detected by the camera at a specific timestamp.
 * Includes the time of detection and a list of detected objects.
 */
public class StampedDetectedObjects 
{
    private int time;
    private List<DetectedObject> detectedObjects;

    public StampedDetectedObjects(int time)
    {
        this.time = time;
        this.detectedObjects = new ArrayList<DetectedObject>();
    }

    public StampedDetectedObjects(int time, List<DetectedObject> detectedObjects)
    {
        this.time = time;
        this.detectedObjects = detectedObjects;
    }

    public int getTime()
    {
        return this.time;
    }

    public synchronized void addDetectedObject(DetectedObject object)
    {
        this.detectedObjects.add(object);
    }

    public List<DetectedObject> getDetectedObjects()
    {
        return List.copyOf(this.detectedObjects);
    }
    
    public List<String> getObjectIDs() {
        List<String> ids = new ArrayList<>();
        for (DetectedObject obj : detectedObjects) {
            ids.add(obj.getId()); // Assuming DetectedObject has a getId() method
        }
        return ids;
    }

    public List<String> getDescriptions() {
        List<String> descriptions = new ArrayList<>();
        for (DetectedObject obj : detectedObjects) {
            descriptions.add(obj.getDescription()); // Assuming DetectedObject has a getDescription() method
        }
        return descriptions;
    }
}
