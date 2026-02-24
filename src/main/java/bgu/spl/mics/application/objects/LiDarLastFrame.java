package bgu.spl.mics.application.objects;

import java.util.List;

public class LiDarLastFrame 
{
    private List<TrackedObject> last_tracked_objcet;
    private String lidar_name;
    
    public LiDarLastFrame(List<TrackedObject> object, String name)
    {
        this.last_tracked_objcet = object;
        this.lidar_name = name;
    }

    public List<TrackedObject> getTrackedObject()
    {
        return this.last_tracked_objcet;
    }

    public String getLidarName()
    {
        return this.lidar_name;
    }
}
