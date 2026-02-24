package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a group of cloud points corresponding to a specific timestamp.
 * Used by the LiDAR system to store and process point cloud data for tracked objects.
 */
public class StampedCloudPoints 
{
    private String id;
    private int time;
    private List<CloudPoint> coordinates;

    public StampedCloudPoints(String id, int time)
    {
        this.id = id;
        this.time = time;
        this.coordinates = new ArrayList<CloudPoint>();
    }

    public StampedCloudPoints(String id,  int time, List<CloudPoint> coordinates)
    {
        this.id = id;
        this.time = time;
        this.coordinates = coordinates;
    }

    public String getId()
    {
        return this.id;
    }

    public int getTime()
    {
        return this.time;
    }

    public synchronized void addCloudPoints(CloudPoint cPoint)
    {
        this.coordinates.add(cPoint);
    }

    public List<CloudPoint> getCoordinates()
    {
        return List.copyOf(this.coordinates);
    }
}
