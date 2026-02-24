package bgu.spl.mics.application.objects;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class OutputFile {
    private List<LandMark> landMarks = new ArrayList<>();
    private String file_directory_path;
    private String error;
    private String faultySensor;
    private List<Pose> poses = new ArrayList<>();
    private List<CameraLastFrame> cameraLastFrame = new ArrayList<>();
    private List<LiDarLastFrame> lidarLastFrame = new ArrayList<>();

    public OutputFile(String direcory_path)
    {
        this.error = null;
        this.faultySensor = null;
        this.file_directory_path = Paths.get(Paths.get(direcory_path).toString(),"output.json").toString();
    }

    public List<LandMark> getLandMarks() {
        return landMarks;
    }

    public void setLandMarks(List<LandMark> landMarks) {
        this.landMarks = landMarks;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getFaultySensor() {
        return faultySensor;
    }

    public void setFaultySensor(String faultySensor) {
        this.faultySensor = faultySensor;
    }

    public List<Pose> getPoses() {
        return poses;
    }

    public void setPoses(List<Pose> poses) {
        this.poses = poses;
    }

    public List<CameraLastFrame> getCameraLastFrame() {
        return cameraLastFrame;
    }

    public void setCameraLastFrame(List<CameraLastFrame> cameraLastFrame) {
        this.cameraLastFrame = cameraLastFrame;
    }

    public List<LiDarLastFrame> getLidarLastFrame() {
        return lidarLastFrame;
    }

    public void setLidarLastFrame(List<LiDarLastFrame> lidarLastFrame) {
        this.lidarLastFrame = lidarLastFrame;
    }

    public String toJsoString()
    {
        return OutputFileSerializer.serialize(this);
    }

    public void uploadFile()
    {
        try (FileWriter file = new FileWriter(this.file_directory_path))
        {
            file.write(this.toJsoString());
            System.out.println("File uploaded to: " + this.file_directory_path);
        } 
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static class OutputFileSerializer {

        public static String serialize(OutputFile outputFile) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject rootNode = new JsonObject();
        
            // Populate basic statistics
            rootNode.addProperty("systemRuntime", StatisticalFolder.getInstance().getSystemRuntime());
            rootNode.addProperty("numDetectedObjects", StatisticalFolder.getInstance().getNumDetectedObjects());
            rootNode.addProperty("numTrackedObjects", StatisticalFolder.getInstance().getNumTrackedObjects());
            rootNode.addProperty("numLandmarks", StatisticalFolder.getInstance().getNumLandmarks());
        
            // Handle errors (if any)
            if (outputFile.getError() != null) {
                rootNode.addProperty("error", outputFile.getError());
                rootNode.addProperty("faultySensor", outputFile.getFaultySensor());
        
                JsonObject lastFramesNode = new JsonObject();
        
                // Camera last frames
                JsonObject cameraFramesNode = new JsonObject();
                for (CameraLastFrame frame : outputFile.getCameraLastFrame()) {
                    JsonObject frameNode = new JsonObject();
                    frameNode.addProperty("time", frame.getDetectedObjects().getTime());
                    JsonArray detectedObjectsArray = new JsonArray();
                    for (DetectedObject obj : frame.getDetectedObjects().getDetectedObjects()) {
                        JsonObject objNode = new JsonObject();
                        objNode.addProperty("id", obj.getId());
                        objNode.addProperty("description", obj.getDescription());
                        detectedObjectsArray.add(objNode);
                    }
                    frameNode.add("detectedObjects", detectedObjectsArray);
                    cameraFramesNode.add(frame.getSenderName(), frameNode);
                }
                lastFramesNode.add("lastCameraFrames", cameraFramesNode);
        
                // Lidar last frames
                JsonObject lidarFramesNode = new JsonObject();
                for (LiDarLastFrame frame : outputFile.getLidarLastFrame()) {
                    if (frame.getTrackedObject() != null && !frame.getTrackedObject().isEmpty()) {
                        JsonObject frameNode = new JsonObject();
                        frameNode.addProperty("lidarName", frame.getLidarName());
                        JsonArray trackedObjectsArray = new JsonArray();
                        for (TrackedObject obj : frame.getTrackedObject()) {
                            JsonObject objNode = gson.toJsonTree(obj).getAsJsonObject();
                            trackedObjectsArray.add(objNode);
                        }
                        frameNode.add("trackedObjects", trackedObjectsArray);
                        lidarFramesNode.add(frame.getLidarName(), frameNode);
                    }
                }
                lastFramesNode.add("lastLidarFrames", lidarFramesNode);
        
                rootNode.add("lastFrames", lastFramesNode);
        
                // Serialize poses
                JsonArray posesArray = new JsonArray();
                for (Pose pose : outputFile.getPoses()) {
                    posesArray.add(gson.toJsonTree(pose));
                }
                rootNode.add("poses", posesArray);
            }
        
            // Serialize landmarks (add as the last field)
            JsonObject landmarksNode = new JsonObject();
            for (LandMark landMark : outputFile.getLandMarks()) {
                JsonObject landmarkNode = new JsonObject();
                landmarkNode.addProperty("id", landMark.getId());
                landmarkNode.addProperty("description", landMark.getDescription());
                JsonArray coordinatesArray = new JsonArray();
                for (CloudPoint coordinate : landMark.getCoordinateList()) {
                    JsonObject coordinateNode = new JsonObject();
                    coordinateNode.addProperty("x", coordinate.getX());
                    coordinateNode.addProperty("y", coordinate.getY());
                    coordinatesArray.add(coordinateNode);
                }
                landmarkNode.add("coordinates", coordinatesArray);
                landmarksNode.add(landMark.getId(), landmarkNode);
            }
            rootNode.add("landMarks", landmarksNode);
        
            return gson.toJson(rootNode);
        }        
    }
}
