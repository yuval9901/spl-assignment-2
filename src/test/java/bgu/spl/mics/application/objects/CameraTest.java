package bgu.spl.mics.application.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CameraTest {

    private Camera camera;

    @BeforeEach
    void setUp() {
        camera = new Camera(1, 60, "CAM123");
    }

    @Test
    void testConstructorAndGetters() {
        // Act
        int id = camera.getId();
        int frequency = camera.getFrequency();
        String cameraKey = camera.getCameraKey();
        STATUS status = camera.getStatus();

        // Assert
        assertEquals(1, id, "Camera ID should be initialized to 1");
        assertEquals(60, frequency, "Camera frequency should be initialized to 60");
        assertEquals("CAM123", cameraKey, "Camera key should be initialized to CAM123");
        assertEquals(STATUS.UP, status, "Camera status should be initialized to UP");
    }

    @Test
    void testAddDetectedObjectAndRetrieveList() {
        // Arrange
        StampedDetectedObjects stamped1 = new StampedDetectedObjects(100);
        StampedDetectedObjects stamped2 = new StampedDetectedObjects(200);

        // Act
        camera.addDetectedObject(stamped1);
        camera.addDetectedObject(stamped2);
        List<StampedDetectedObjects> detectedObjectsList = camera.getListOfDetectedObjects();

        // Assert
        assertEquals(2, detectedObjectsList.size(), "Detected objects list size should be 2");
        assertTrue(detectedObjectsList.contains(stamped1), "Detected objects list should contain stamped1");
        assertTrue(detectedObjectsList.contains(stamped2), "Detected objects list should contain stamped2");
    }

    @Test
    void testSetStatus() {
        // Act
        camera.setStatus(STATUS.DOWN);

        // Assert
        assertEquals(STATUS.DOWN, camera.getStatus(), "Camera status should be updated to DOWN");
    }

    @Test
    void testPrepareDataBeforeSending() {
        // Arrange
        DetectedObject obj1 = new DetectedObject("OBJ1", "Tree");
        DetectedObject obj2 = new DetectedObject("OBJ2", "Car");
        List<DetectedObject> detectedObjects = new ArrayList<>();
        detectedObjects.add(obj1);
        detectedObjects.add(obj2);
        StampedDetectedObjects stampedDetectedObjects = new StampedDetectedObjects(300, detectedObjects);
        camera.addDetectedObject(stampedDetectedObjects);

        // Act
        List<StampedDetectedObjects> detectedList = camera.getListOfDetectedObjects();
        List<String> objectIDs = detectedList.get(0).getObjectIDs();
        List<String> descriptions = detectedList.get(0).getDescriptions();

        // Assert
        assertEquals(1, detectedList.size(), "Detected objects list size should be 1");
        assertEquals(2, objectIDs.size(), "Object IDs list size should be 2");
        assertTrue(objectIDs.contains("OBJ1"), "Object IDs list should contain OBJ1");
        assertTrue(objectIDs.contains("OBJ2"), "Object IDs list should contain OBJ2");
        assertTrue(descriptions.contains("Tree"), "Descriptions list should contain 'Tree'");
        assertTrue(descriptions.contains("Car"), "Descriptions list should contain 'Car'");
    }

    @Test
    void testDetectedObjectsManagement() {
        // Arrange
        StampedDetectedObjects stamped1 = new StampedDetectedObjects(500);
        StampedDetectedObjects stamped2 = new StampedDetectedObjects(600);

        // Act
        camera.addDetectedObject(stamped1);
        camera.addDetectedObject(stamped2);
        List<StampedDetectedObjects> objectsList = camera.getListOfDetectedObjects();

        // Assert
        assertEquals(2, objectsList.size(), "Detected objects list size should be 2");
        assertEquals(500, objectsList.get(0).getTime(), "First detected object's time should be 500");
        assertEquals(600, objectsList.get(1).getTime(), "Second detected object's time should be 600");
    }
}