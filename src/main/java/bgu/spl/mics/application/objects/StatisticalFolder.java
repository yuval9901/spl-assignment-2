package bgu.spl.mics.application.objects;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds statistical information about the system's operation.
 * This class aggregates metrics such as the runtime of the system,
 * the number of objects detected and tracked, and the number of landmarks identified.
 */
public class StatisticalFolder {
    private AtomicInteger systemRuntime;         
    private AtomicInteger numDetectedObjects;  
    private AtomicInteger numTrackedObjects;    
    private AtomicInteger numLandmarks;         

    private static class StaticalFolderHolder {
        private static final StatisticalFolder INSTANCE = new StatisticalFolder();
    } 

    private StatisticalFolder() {
        this.systemRuntime = new AtomicInteger();
        this.numDetectedObjects = new AtomicInteger();
        this.numTrackedObjects = new AtomicInteger();
        this.numLandmarks = new AtomicInteger();
    }

    public static StatisticalFolder getInstance() {
        return StaticalFolderHolder.INSTANCE;
    }

    public int getSystemRuntime() {
        return this.systemRuntime.get();
    }

    public int getNumDetectedObjects() {
        return this.numDetectedObjects.get();
    }

    public int getNumTrackedObjects() {
        return this.numTrackedObjects.get();
    }

    public int getNumLandmarks() {
        return this.numLandmarks.get();
    }

    public void incrementSystemRuntime() {
        this.systemRuntime.incrementAndGet();
    }

    public void incrementDetectedObjects(int count) {
        this.numDetectedObjects.addAndGet(count);
    }

    public void incrementTrackedObjects(int count) {
        this.numTrackedObjects.addAndGet(count);
    }

    public void incrementLandmarks() {
        this.numLandmarks.getAndIncrement();
    }

    @Override
    public synchronized String toString() {
        return "StatisticalFolder{" +
                "systemRuntime=" + systemRuntime +
                ", numDetectedObjects=" + numDetectedObjects +
                ", numTrackedObjects=" + numTrackedObjects +
                ", numLandmarks=" + numLandmarks +
                '}';
    }
}
