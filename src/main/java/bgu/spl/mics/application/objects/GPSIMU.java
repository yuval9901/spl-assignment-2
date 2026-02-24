package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the robot's GPS and IMU system.
 * Provides information about the robot's position and movement.
 */
public class GPSIMU 
{
    private int currentTick;
    private STATUS status;
    private List<Pose> poseList;

    public GPSIMU(int currentTick, STATUS status)
    {
        this.currentTick = currentTick;
        this.status = status;
        this.poseList = new ArrayList<Pose>();
    }

    public GPSIMU(int currentTick, STATUS status, List<Pose> poseList)
    {
        this.currentTick = currentTick;
        this.status = status;
        this.poseList = poseList;
    }

    public int getCurrentTick()
    {
        return this.currentTick;
    }

    public STATUS getStatus()
    {
        return this.status;
    }

    public void setStatus(STATUS status)
    {
        this.status = status;
    }

    public synchronized void addPose(Pose pose)
    {
        this.poseList.add(pose);
    }
    public List<Pose> getListOPoses()
    {
        return List.copyOf(this.poseList);
    }
}
