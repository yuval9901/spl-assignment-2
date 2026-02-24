package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a landmark in the environment map.
 * Landmarks are identified and updated by the FusionSlam service.
 */
public class LandMark 
{
    private String id;
    private String description;
    private List<CloudPoint> coordinates;

    public LandMark(String id, String description, List<CloudPoint> coordinates)
    {
        this.id = id;
        this.description = description;
        this.coordinates = coordinates;
    }

    public LandMark(String id, String description)
    {
        this.id = id;
        this.description = description;
        coordinates = new ArrayList<CloudPoint>();
    }

    public String getId()
    {
        return this.id;
    }

    public String getDescription()
    {
        return this.description;
    }

    public synchronized void addCoordinate(CloudPoint cPoint)
    {
        this.coordinates.add(cPoint);
    }

    public List<CloudPoint> getCoordinateList()
    {
        return List.copyOf(this.coordinates);
    }

    public void setCoordinates(List<CloudPoint> list)
    {
        this.coordinates = list;
    }
}
