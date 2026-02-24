package bgu.spl.mics.application.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.EndTimeBroadcast;
import bgu.spl.mics.application.messages.LastFrameBroadcast;
import bgu.spl.mics.application.messages.MicroServiceChangeStateBroadcast;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.CameraLastFrame;
import bgu.spl.mics.application.objects.CloudPoint;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.LandMark;
import bgu.spl.mics.application.objects.LiDarLastFrame;
import bgu.spl.mics.application.objects.OutputFile;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.StatisticalFolder;
import bgu.spl.mics.application.objects.TrackedObject;

public class FusionSlamService extends MicroService {
    private List<MicroService> connectedServices;
    private final FusionSlam fusionSlam;
    private OutputFile output;
    private final Map<Integer, List<TrackedObject>> trackedObjectBuffer = new HashMap<>();
    private CountDownLatch latch;

    public FusionSlamService(FusionSlam fusionSlam, String output_file_directory_path, CountDownLatch fusionInitLatch) {
        super("FusionSlamService");
        this.fusionSlam = fusionSlam;
        this.connectedServices = new ArrayList<>();
        this.output = new OutputFile(output_file_directory_path);
        this.latch = fusionInitLatch;
    }

    @Override
    protected void initialize() {
        // Subscriptions and service logic
        subscribeBroadcast(MicroServiceChangeStateBroadcast.class, event -> {
            if (!event.getIsEnded()) {
                this.connectedServices.add(event.getService());
            } else {
                this.connectedServices.remove(event.getService());
            }
        });

        subscribeEvent(TrackedObjectsEvent.class, trackedObjectsEvent -> {
            List<TrackedObject> list_of_objects = new ArrayList<>(trackedObjectsEvent.getTrackedObject());
            for (TrackedObject object : list_of_objects) {
                int objectTime = object.getTime();

                Pose robotPose = fusionSlam.getPoseAtTime(objectTime);

                if (robotPose != null) {
                    processTrackedObject(object, robotPose);
                } else {
                    trackedObjectBuffer.computeIfAbsent(objectTime, k -> new ArrayList<>()).add(object);
                }
                System.out.println("Fusion SLAM added " + object.getId() + " " + object.getDescription());
            }
        });

        subscribeEvent(PoseEvent.class, poseEvent -> {
            Pose newPose = poseEvent.getPose();
            int poseTime = newPose.getTime();
            fusionSlam.addPose(newPose);

            List<TrackedObject> objectsForPose = trackedObjectBuffer.remove(poseTime);
            if (objectsForPose != null) {
                for (TrackedObject object : objectsForPose) {
                    processTrackedObject(object, newPose);
                    System.out.println("Fusion SLAM added object that was waiting");
                }
            }
            System.out.println("Fusion SLAM added pose");
        });

        // Handle TerminatedBroadcast
        subscribeBroadcast(TerminatedBroadcast.class, broadcast -> {
            if (!(this.connectedServices.isEmpty() && MessageBusImpl.getInstance().isRegistered(LiDarService.class, this))) {
                this.sendBroadcast(new TerminatedBroadcast(getName()));
            } else {
                this.output.setLandMarks(this.fusionSlam.getLandmarks());
                this.output.uploadFile();
                this.terminate();
            }
        });

        subscribeBroadcast(LastFrameBroadcast.class, broadcast -> {
            if (broadcast.getCameraLastFrame() != null) {
                this.output.getCameraLastFrame().add(new CameraLastFrame(broadcast.getCameraLastFrame(), broadcast.getSender()));
            } else {
                this.output.getLidarLastFrame().add(new LiDarLastFrame(broadcast.getLidarLastFrame(), broadcast.getSender()));
            }
        });

        // Handle CrashedBroadcast
        subscribeBroadcast(CrashedBroadcast.class, broadcast -> {
            this.output.setFaultySensor(broadcast.getSenderServiceName());
            this.output.setError(broadcast.getDescription());
            this.output.setPoses(this.fusionSlam.getPoses());
            this.output.setLandMarks(this.fusionSlam.getLandmarks());
            this.output.uploadFile();
            this.sendBroadcast(new TerminatedBroadcast(getName()));
        });

        // Handle TickBroadcast (optional for future implementations, not currently used)
        subscribeBroadcast(TickBroadcast.class, tickBroadcast -> {
            if (this.connectedServices.isEmpty() && MessageBusImpl.getInstance().isRegistered(LiDarService.class, this)) {
                this.sendBroadcast(new EndTimeBroadcast());
                this.sendBroadcast(new TerminatedBroadcast(getName()));
            }
        });
        this.latch.countDown();
        System.out.println("Fusion SLAM finished init");
    }

    private void processTrackedObject(TrackedObject object, Pose pose) {
        boolean isNewLandmark = true;

        // Step 1: Transform new coordinates to global system
        List<CloudPoint> newCoordinates = object.getCoordinates();
        List<CloudPoint> transformedCoordinates = new ArrayList<>();

        double thetaRad = Math.toRadians(pose.getYaw());
        double cosTheta = Math.cos(thetaRad);
        double sinTheta = Math.sin(thetaRad);
        double xRobot = pose.getX();
        double yRobot = pose.getY();

        for (CloudPoint point : newCoordinates) {
            double xLocal = point.getX();
            double yLocal = point.getY();

            double xGlobal = cosTheta * xLocal - sinTheta * yLocal + xRobot;
            double yGlobal = sinTheta * xLocal + cosTheta * yLocal + yRobot;

            transformedCoordinates.add(new CloudPoint(xGlobal, yGlobal));
        }

        // Step 2: Check if landmark exists
        for (LandMark landMark : this.fusionSlam.getLandmarks()) {
            if (landMark.getId().equals(object.getId())) {
                isNewLandmark = false;

                List<CloudPoint> existingCoordinates = landMark.getCoordinateList();
                List<CloudPoint> updatedCoordinates = new ArrayList<>();

                int sizeL = existingCoordinates.size();
                int sizeC = transformedCoordinates.size();
                int k = Math.min(sizeL, sizeC);

                for (int i = 0; i < k; i++) {
                    CloudPoint lPoint = existingCoordinates.get(i);
                    CloudPoint cPoint = transformedCoordinates.get(i);

                    double avgX = 0.5 * (lPoint.getX() + cPoint.getX());
                    double avgY = 0.5 * (lPoint.getY() + cPoint.getY());
                    updatedCoordinates.add(new CloudPoint(avgX, avgY));
                }

                if (sizeC > k) {
                    updatedCoordinates.addAll(transformedCoordinates.subList(k, sizeC));
                }

                if (sizeL > k) {
                    updatedCoordinates.addAll(existingCoordinates.subList(k, sizeL));
                }

                landMark.setCoordinates(updatedCoordinates);
                break;
            }
        }

        if (isNewLandmark) {
            StatisticalFolder.getInstance().incrementLandmarks();
            LandMark newLandMark = new LandMark(object.getId(), object.getDescription(), transformedCoordinates);
            this.fusionSlam.addLandmark(newLandMark);
        }
    }
}
