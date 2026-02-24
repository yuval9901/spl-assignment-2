package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

public class CrashedBroadcast implements Broadcast {

    private final String senderServiceName;
    private final int tick;
    private final String description;

    public CrashedBroadcast(String senderServiceName,int tick, String description) 
    {
        this.senderServiceName = senderServiceName;
        this.tick = tick;
        this.description = description;
    }

    public String getSenderServiceName() {
        return senderServiceName;
    }

    public int getTick()
    {
        return this.tick;
    }

    public String getDescription()
    {
        return this.description;
    }
}
