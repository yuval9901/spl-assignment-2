package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.Pose;

public class PoseEvent implements Event<Pose>
{
    private final Pose currentPose;
    private String senderName;
    
    public PoseEvent(Pose currentPose, String senderName)
    {
        this.currentPose = currentPose;
        this.senderName = senderName;
    }

    public Pose getPose()
    {
        return this.currentPose;
    }

    public String getSenderName()
    {
        return this.senderName;
    }
}
