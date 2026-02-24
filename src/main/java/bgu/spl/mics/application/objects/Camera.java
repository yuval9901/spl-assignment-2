package bgu.spl.mics.application.objects;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Represents a camera sensor on the robot.
 * Responsible for detecting objects in the environment.
 */
public class Camera {
    @SerializedName("id")
    private int id;
    @SerializedName("frequency")
    private int frequency;
    private STATUS status = STATUS.UP;
    private List<StampedDetectedObjects> detectedObjectsList = new ArrayList<>();
    @SerializedName("camera_key")
    private String cameraKey; // New field for camera_key
    
    public Camera(int id, int frequency, String cameraKey) {
        this.id = id;
        this.frequency = frequency;
        this.cameraKey = cameraKey;
    }

    public Camera(int id, int frequency, STATUS status, String cameraKey, List<StampedDetectedObjects> detectedObjects) {
        this.id = id;
        this.frequency = frequency;
        this.status = status;
        this.detectedObjectsList = detectedObjects;
        this.cameraKey = cameraKey;
    }

    public synchronized void addDetectedObject(StampedDetectedObjects object) {
        this.detectedObjectsList.add(object);
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

    public List<StampedDetectedObjects> getListOfDetectedObjects() {
        return this.detectedObjectsList;
    }

    public String getCameraKey() {
        return this.cameraKey; // Getter for camera_key
    }

    public void setStatus(STATUS status)
    {
        this.status = status;
    }
}
