package bgu.spl.mics.application.objects;

import java.util.List;
import java.util.ArrayList;

/**
 * Manages the fusion of sensor data for simultaneous localization and mapping (SLAM).
 * Combines data from multiple sensors (e.g., LiDAR, camera) to build and update a global map.
 * Implements the Singleton pattern to ensure a single instance of FusionSlam exists.
 */
public class FusionSlam 
{

    // Fields
    private final List<LandMark> landmarks; // Represents the map of the environment
    private final List<Pose> poses; // Represents previous poses needed for calculations
    private Pose currentPose;

    // Private constructor to enforce Singleton pattern
    private FusionSlam() {
        this.landmarks = new ArrayList<>();
        this.poses = new ArrayList<>();
        this.currentPose = null;
    }

    /**
     * Singleton instance holder class.
     * Ensures thread-safe lazy initialization of the FusionSlam instance.
     */
    private static class FusionSlamHolder {
        private static final FusionSlam INSTANCE = new FusionSlam();
    }

    /**
     * Provides the singleton instance of FusionSlam.
     * 
     * @return the singleton FusionSlam instance.
     */
    public static FusionSlam getInstance() {
        return FusionSlamHolder.INSTANCE;
    }

    // Methods

    /**
     * Adds a new landmark to the map.
     * 
     * @param landmark the Landmark to add.
     */
    public synchronized void addLandmark(LandMark landmark) {
        landmarks.add(landmark);
    }

    /**
     * Retrieves the list of landmarks.
     * 
     * @return the list of landmarks.
     */
    public synchronized List<LandMark> getLandmarks() {
        return new ArrayList<>(landmarks);
    }

    /**
     * Adds a new pose to the list of previous poses.
     * 
     * @param pose the Pose to add.
     */
    public synchronized void addPose(Pose pose) {
        poses.add(pose);
        this.currentPose = pose;
    }

    /**
     * Retrieves the list of previous poses.
     * 
     * @return the list of poses.
     */
    public synchronized List<Pose> getPoses() 
    {
        return new ArrayList<>(poses);
    }

    public synchronized void setCurrentPose(Pose pose)
    {
        this.currentPose = pose;
    }

    public synchronized Pose getCurrentPose()
    {
        return this.currentPose;
    }

    public synchronized Pose getPoseAtTime(int objectTime) 
    {
        for(Pose p : this.poses)
        {
            if(p.getTime() == objectTime)
            {
                return p;
            }
        }
        return null;
    }

    public synchronized void cleanPoses()
    {
        this.poses.clear();
        this.currentPose = null;
    }
}
