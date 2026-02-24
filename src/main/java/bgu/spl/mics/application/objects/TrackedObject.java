package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an object tracked by the LiDAR.
 * This object includes information about the tracked object's ID, description, 
 * time of tracking, and coordinates in the environment.
 */
public class TrackedObject 
{
    private String id;
    private String description;
    private int time;
    private List<CloudPoint> coordinates;

    public TrackedObject(String id, String description, int time, List<CloudPoint> coordinates)
    {
        this.id = id;
        this.description = description;
        this.time = time;
        this.coordinates = coordinates;
    }

    public TrackedObject(String id, String description, int time)
    {
        this.id = id;
        this.description = description;
        this.time = time;
        this.coordinates = new ArrayList<CloudPoint>();
    }

    public String getId()
    {
        return this.id;
    }

    public String getDescription()
    {
        return this.description;
    }

    public int getTime()
    {
        return this.time;
    }

    public void addCoordinate(CloudPoint point)
    {
        this.coordinates.add(point);
    }

    public List<CloudPoint> getCoordinates()
    {
        return List.copyOf(this.coordinates);
    }
}
