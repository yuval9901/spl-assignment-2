package bgu.spl.mics.application.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FusionSlamTest {

    private FusionSlam fusionSlam;

    @BeforeEach
    void setUp() {
        // Reset the singleton instance for testing purposes
        fusionSlam = FusionSlam.getInstance();
        fusionSlam.cleanPoses();
    }

    @Test
    void testSingletonInstance() {
        // Ensure the same instance is returned every time
        FusionSlam instance1 = FusionSlam.getInstance();
        FusionSlam instance2 = FusionSlam.getInstance();

        assertSame(instance1, instance2, "FusionSlam should return the same instance");
    }

    @Test
    void testAddLandmarkAndRetrieveLandmarks() {
        // Arrange
        List<CloudPoint> coords1 = new ArrayList<>();
        coords1.add(new CloudPoint(10.0, 20.0));
        LandMark landmark1 = new LandMark("LM1", "First landmark", coords1);

        List<CloudPoint> coords2 = new ArrayList<>();
        coords2.add(new CloudPoint(40.0, 50.0));
        LandMark landmark2 = new LandMark("LM2", "Second landmark", coords2);

        // Act
        fusionSlam.addLandmark(landmark1);
        fusionSlam.addLandmark(landmark2);
        List<LandMark> landmarks = fusionSlam.getLandmarks();

        // Assert
        assertEquals(2, landmarks.size(), "Landmarks list size should be 2");
        assertTrue(landmarks.contains(landmark1), "Landmarks should contain landmark1");
        assertTrue(landmarks.contains(landmark2), "Landmarks should contain landmark2");
    }

    @Test
    void testAddPoseAndRetrievePoses() 
    {
        // Arrange
        Pose pose1 = new Pose(0, 0, 0, 1);
        Pose pose2 = new Pose(10, 10, 90, 2);

        // Act
        fusionSlam.addPose(pose1);
        fusionSlam.addPose(pose2);
        List<Pose> poses = fusionSlam.getPoses();

        // Assert
        assertEquals(2, poses.size(), "Poses list size should be 2");
        assertTrue(poses.contains(pose1), "Poses should contain pose1");
        assertTrue(poses.contains(pose2), "Poses should contain pose2");
    }

    @Test
    void testGetPoseAtTime() {
        // Arrange
        Pose pose1 = new Pose(0, 0, 0, 1);
        Pose pose2 = new Pose(10, 10, 90, 2);
        fusionSlam.addPose(pose1);
        fusionSlam.addPose(pose2);

        // Act
        Pose result = fusionSlam.getPoseAtTime(2);

        // Assert
        assertNotNull(result, "Pose at time 2 should not be null");
        assertEquals(pose2, result, "Pose at time 2 should match pose2");
    }

    @Test
    void testSetAndGetCurrentPose() {
        // Arrange
        Pose pose = new Pose(5, 5, 45, 3);

        // Act
        fusionSlam.setCurrentPose(pose);
        Pose currentPose = fusionSlam.getCurrentPose();

        // Assert
        assertEquals(pose, currentPose, "Current pose should match the set pose");
    }

    @Test
    void testGetPoseAtInvalidTime() {
        // Act
        Pose result = fusionSlam.getPoseAtTime(999);

        // Assert
        assertNull(result, "Pose at invalid time should be null");
    }

    @Test
    void testLandmarkCoordinateManagement() {
        // Arrange
        LandMark landmark = new LandMark("LM1", "Test landmark");
        CloudPoint point1 = new CloudPoint(1.5, 2.5);
        CloudPoint point2 = new CloudPoint(4.5, 5.5);

        // Act
        landmark.addCoordinate(point1);
        landmark.addCoordinate(point2);
        List<CloudPoint> coordinates = landmark.getCoordinateList();

        // Assert
        assertEquals(2, coordinates.size(), "Coordinate list size should be 2");
        assertTrue(coordinates.contains(point1), "Coordinates should contain point1");
        assertTrue(coordinates.contains(point2), "Coordinates should contain point2");
    }

    @Test
    void testCloudPointMutability() {
        // Arrange
        CloudPoint point = new CloudPoint(3.0, 4.0);

        // Act
        point.setX(6.0);
        point.setY(8.0);

        // Assert
        assertEquals(6.0, point.getX(), "CloudPoint X coordinate should be updated to 6.0");
        assertEquals(8.0, point.getY(), "CloudPoint Y coordinate should be updated to 8.0");
    }
}