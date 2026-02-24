package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * LiDarWorkerTracker is responsible for managing a LiDAR worker.
 * It processes DetectObjectsEvents and generates TrackedObjectsEvents by using data from the LiDarDataBase.
 * Each worker tracks objects and sends observations to the FusionSLAM service.
 */
public class LiDarWorkerTracker {
    @SerializedName("id")
    private int id;
    @SerializedName("frequency")
    private int frequency;
    private STATUS status = STATUS.UP;
    private List<TrackedObject> lastaTrackedObjects = new ArrayList<>();

    public LiDarWorkerTracker(int id, int frequency) {
        this.id = id;
        this.frequency = frequency;
    }

    public int getId() {
        return this.id;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public STATUS getStatus() {
        return this.status;
    }

    public void setStatus(STATUS status)
    {
        this.status = status;
    }

    public List<TrackedObject> getLastTrackedObjects() {
        return this.lastaTrackedObjects;
    }

    /**
     * Adds a tracked object to the list of last tracked objects.
     *
     * @param trackedObject The tracked object to add.
     */
    public void addTrackedObject(TrackedObject trackedObject) {
        this.lastaTrackedObjects.add(trackedObject);
    }
}
