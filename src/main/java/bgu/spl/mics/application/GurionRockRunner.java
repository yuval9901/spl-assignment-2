package bgu.spl.mics.application;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.CameraService;
import bgu.spl.mics.application.services.FusionSlamService;
import bgu.spl.mics.application.services.LiDarService;
import bgu.spl.mics.application.services.PoseService;
import bgu.spl.mics.application.services.TimeService;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class GurionRockRunner {

    public static void main(String[] args) {
        // Initialize lists and gson object
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Camera.class, new CameraDeserializer())
            .registerTypeAdapter(LiDarWorkerTracker.class, new LiDarWorkerTrackerDeserializer())
            .create();
        LiDarDataBase liDarDataBase = LiDarDataBase.getInstance();
        List<Pose> poses = new ArrayList<>();
        Config config = null ;

        System.out.println("Working Directory: " + System.getProperty("user.dir"));



        Path input_file_path = Paths.get(args[0]);


        try (FileReader reader = new FileReader(input_file_path.toString())) {
            // Deserialize JSON to a full JsonObject
            config = gson.fromJson(reader, Config.class);
        }
        catch(Exception ex)
        {
            System.err.println(ex);
        }

        try(FileReader reader = new FileReader(config.cameras.getPath()))
        {
            Map<String, List<StampedDetectedObjects>> cameraData = parseCameraData(reader, gson);
            for(Camera camera : config.cameras.cameras)
            {
                for(StampedDetectedObjects stamp : cameraData.get(camera.getCameraKey()))
                {
                    camera.addDetectedObject(stamp);
                }
            }
        }
        catch(Exception ex)
        {
            System.out.println(ex);
        }

        try (FileReader reader = new FileReader(config.lidars.getPath())) {
            System.out.println("Lidar data file path: " + config.lidars.getPath());
            List<LidarData> lidarData = parseLidarData(reader, gson);
            
            for (LidarData data : lidarData) {
                List<CloudPoint3D> cloudPoints3D = new ArrayList<>();
                for (double[] point : data.getCloudPoints()) {
                    cloudPoints3D.add(new CloudPoint3D(point[0], point[1], point[2]));
                }
                
                // Convert to 2D
                List<CloudPoint> cloudPoints2D = convert3DTo2D(cloudPoints3D);
        
                // Create StampedCloudPoints object and add to database
                StampedCloudPoints stampedPoints = new StampedCloudPoints(data.getId(), data.getTime(), cloudPoints2D);
                liDarDataBase.addCloudPoints(stampedPoints);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try(FileReader reader = new FileReader(config.pose_data_file_path))
        {
            Type lidarDataListType = new TypeToken<List<Pose>>() {}.getType();
            poses = gson.fromJson(reader, lidarDataListType);
        }
        catch(Exception ex)
        {
            System.err.println(ex);
        }

        CountDownLatch startLatch = new CountDownLatch(1+config.cameras.cameras.size()+config.lidars.lidars.size());

        // Initialize and start FusionSlamService (waits on fusionInitLatch for initialization)
        CountDownLatch fusionInitLatch = new CountDownLatch(1);
        FusionSlamService fService = new FusionSlamService(FusionSlam.getInstance(), input_file_path.getParent().toString(), fusionInitLatch);
        Thread fusionThread = new Thread(fService);
        fusionThread.start();

        // Wait for FusionSlamService initialization
        try{
            fusionInitLatch.await();
        }
        catch(InterruptedException ex)
        {
            System.err.println(ex);
        }

        
        // Initialize other services (cameras, lidar)
        List<Thread> cameraThreads = new ArrayList<>();
        for (Camera c : config.cameras.cameras) {
            Thread cThread = new Thread(new CameraService(c, startLatch));
            cameraThreads.add(cThread);
            cThread.start();
        }

        List<Thread> lidarThreads = new ArrayList<>();
        for (LiDarWorkerTracker lidar : config.lidars.lidars) {
            Thread l = new Thread(new LiDarService(lidar, startLatch));
            lidarThreads.add(l);
            l.start();
        }
        PoseService poseService = new PoseService(new GPSIMU(0, STATUS.UP, poses),startLatch);
        Thread poseThread = new Thread(poseService);
        poseThread.start();

        try {
            startLatch.await();
        } catch (InterruptedException ex) {
            System.err.println(ex);
        }

        // Initialize and start TimeService
        TimeService timeService = new TimeService(config.TickTime * 1000, config.Duration);
        Thread timeThread = new Thread(timeService);
        timeThread.start();

        // Wait for all threads to complete
        try {
            fusionThread.join();
            for (Thread cThread : cameraThreads) {
                cThread.join();
            }
            for (Thread l : lidarThreads) {
                l.join();
            }
            timeThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Print final results
        System.out.println("Initialized cameras: " + config.cameras.cameras.size());
        System.out.println("Initialized LiDAR workers: " + config.lidars.lidars.size());
        System.out.println("Loaded poses: " + poses.size());
    }

    // Configuration classes for Cameras and LiDars

    public static class Config
    {
        @SerializedName("Cameras")
        private CameraConfig cameras;
        @SerializedName("LidarWorkers")
        private LidarConfig lidars;
        @SerializedName("poseJsonFile")
        private String pose_data_file_path;
        @SerializedName("TickTime")
        private int TickTime;
        @SerializedName("Duration")
        private int Duration;

        public Config(CameraConfig cameraConfig, LidarConfig lidarConfig, String path, int tickTime,int duration)
        {
            this.cameras = cameraConfig;
            this.lidars = lidarConfig;
            this.pose_data_file_path = path;
            this.TickTime = tickTime;
            this.Duration = duration;
        }
    }

    public static class CameraConfig 
    {
        @SerializedName("CamerasConfigurations")
        private List<Camera> cameras;
        @SerializedName("camera_datas_path")
        private String camera_data_file_path;

        public List<Camera> getCameraList()
        {
            return this.cameras;
        }

        public String getPath()
        {
            return this.camera_data_file_path;
        }
    }

    public static class CameraData {
        private int time;
        private List<DetectedObject> detectedObjects;

        public int getTime() {
            return time;
        }

        public List<DetectedObject> getDetectedObjects() {
            return detectedObjects;
        }
    }

    public static class LidarConfig {
        @SerializedName("LidarConfigurations")
        private List<LiDarWorkerTracker> lidars;
        @SerializedName("lidars_data_path")
        private String lidar_data_file_path;

        public LidarConfig(List<LiDarWorkerTracker> lidars, String path)
        {
            this.lidars = lidars;
            this.lidar_data_file_path = path;
        }

        public List<LiDarWorkerTracker> getLidars()
        {
            return this.lidars;
        }

        public String getPath()
        {
            return this.lidar_data_file_path;
        }
    }

    // Wrapper class for 3D CloudPoints
    public static class StampedCloudPoints3D {
        private String id;
        private int time;
        private List<CloudPoint3D> cloudPoints;

        public StampedCloudPoints3D(String id, int time, List<CloudPoint3D> cloudPoints) {
            this.id = id;
            this.time = time;
            this.cloudPoints = cloudPoints;
        }

        public String getId() {
            return id;
        }

        public int getTime() {
            return time;
        }

        public List<CloudPoint3D> getCloudPoints() {
            return cloudPoints;
        }
    }

    // Class representing 3D CloudPoint
    public static class CloudPoint3D {
        private double x;
        private double y;
        private double z;

        public CloudPoint3D(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }
    }

    // LiDAR Data Class
    public static class LidarData {
        private int time;
        private String id;
        private List<double[]> cloudPoints;

        public int getTime() {
            return time;
        }

        public String getId() {
            return id;
        }

        public List<double[]> getCloudPoints() {
            return cloudPoints;
        }
    }

    public static class CameraDeserializer implements JsonDeserializer<Camera> {

        @Override
        public Camera deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            // Extract fields from JSON
            int id = jsonObject.get("id").getAsInt();
            int frequency = jsonObject.get("frequency").getAsInt();
            String cameraKey = jsonObject.has("camera_key") ? jsonObject.get("camera_key").getAsString() : "";

            // Use your preferred constructor with default arguments
            return new Camera(id, frequency, cameraKey);
        }
    }

    public static class LiDarWorkerTrackerDeserializer implements JsonDeserializer<LiDarWorkerTracker> {

        @Override
        public LiDarWorkerTracker deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
    
            // Extract the fields from the JSON
            int id = jsonObject.get("id").getAsInt();
            int frequency = jsonObject.get("frequency").getAsInt();
    
            // Use your preferred constructor, setting default values for other fields
            LiDarWorkerTracker lidarWorkerTracker = new LiDarWorkerTracker(id, frequency);
            return lidarWorkerTracker;
        }
    }

    public static Map<String, List<StampedDetectedObjects>> parseCameraData(FileReader reader, Gson gson) {
        Map<String, List<StampedDetectedObjects>> cameraDataMap = new HashMap<>();
    
        // Deserialize the JSON as a generic Map
        JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
    
        // Loop through each camera key in the JSON
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String cameraKey = entry.getKey();
            JsonElement cameraDataElement = entry.getValue();
    
            // Initialize the list to store StampedDetectedObjects objects
            List<StampedDetectedObjects> stampedDetectedObjectsList = new ArrayList<>();
    
            // Check if the value is a JsonArray (for list of CameraData)
            if (cameraDataElement.isJsonArray()) {
                JsonArray cameraDataArray = cameraDataElement.getAsJsonArray();
    
                // Check if the first item in the array is itself a JsonArray (list of lists)
                if (cameraDataArray.size() > 0 && cameraDataArray.get(0).isJsonArray()) {
                    // If it's a list of lists, we need to flatten it
                    for (JsonElement subListElement : cameraDataArray) {
                        JsonArray innerArray = subListElement.getAsJsonArray();
                        for (JsonElement innerItem : innerArray) {
                            // Deserialize each item to CameraData first
                            CameraData cameraData = gson.fromJson(innerItem, CameraData.class);
                            // Then map it to StampedDetectedObjects
                            StampedDetectedObjects stampedDetectedObjects = convertToStampedDetectedObjects(cameraData);
                            stampedDetectedObjectsList.add(stampedDetectedObjects);
                        }
                    }
                } else {
                    // If it's a plain list, process it directly
                    for (JsonElement cameraDataItem : cameraDataArray) {
                        CameraData cameraData = gson.fromJson(cameraDataItem, CameraData.class);
                        // Convert CameraData to StampedDetectedObjects
                        StampedDetectedObjects stampedDetectedObjects = convertToStampedDetectedObjects(cameraData);
                        stampedDetectedObjectsList.add(stampedDetectedObjects);
                    }
                }
            }
    
            // Add the processed camera data to the map
            cameraDataMap.put(cameraKey, stampedDetectedObjectsList);
        }
    
        return cameraDataMap;
    }
    
    // Helper method to convert CameraData to StampedDetectedObjects
    private static StampedDetectedObjects convertToStampedDetectedObjects(CameraData cameraData) {
        List<DetectedObject> detectedObjects = cameraData.getDetectedObjects();
        return new StampedDetectedObjects(cameraData.getTime(), detectedObjects);
    }

    public static List<LidarData> parseLidarData(FileReader reader, Gson gson) {
        Type lidarDataListType = new TypeToken<List<LidarData>>() {}.getType();
        return gson.fromJson(reader, lidarDataListType);
    }

    public static List<CloudPoint> convert3DTo2D(List<CloudPoint3D> cloudPoints3D) {
        List<CloudPoint> cloudPoints2D = new ArrayList<>();
        for (CloudPoint3D point : cloudPoints3D) {
            cloudPoints2D.add(new CloudPoint(point.getX(), point.getY()));
        }
        return cloudPoints2D;
    }

}