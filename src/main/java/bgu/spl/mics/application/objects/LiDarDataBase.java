package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for tracked objects.
 */
public class LiDarDataBase {

    // Fields
    private final Map<Integer,List<StampedCloudPoints>> cloudPoints; // The coordinates we have for every object per time
    private static LiDarDataBase instance = null; // Singleton instance

    /**
     * Private constructor to enforce the Singleton pattern.
     */
    private LiDarDataBase() {
        this.cloudPoints = new HashMap<>();
    }

    /**
     * Returns the singleton instance of LiDarDataBase.
     *
     * @param filePath The path to the LiDAR data file.
     *                 This parameter is optional after the first call.
     * @return The singleton instance of LiDarDataBase.
     */
    public static synchronized LiDarDataBase getInstance() {
        if (instance == null) {
            instance = new LiDarDataBase();
        }
        return instance;
    }

    /**
     * Adds new cloud points data.
     *
     * @param stampedCloudPoints The StampedCloudPoints to add.
     */
    public synchronized void addCloudPoints(StampedCloudPoints stampedCloudPoints) 
    {
        cloudPoints.putIfAbsent(stampedCloudPoints.getTime(), new ArrayList<>());
        // Add the stampedCloudPoints to the list
        cloudPoints.get(stampedCloudPoints.getTime()).add(stampedCloudPoints);
    }

    /**
     * Retrieves the list of all cloud points.
     *
     * @return A copy of the cloud points list.
     */
    public synchronized List<StampedCloudPoints> getCloudPoints(int time) 
    {
        List<StampedCloudPoints> points = cloudPoints.get(time);
        return List.copyOf(points);
    }
    
    /**
     * Clears all stored cloud points.
     */
    public synchronized void clearCloudPoints() {
        cloudPoints.clear();
    }
}
