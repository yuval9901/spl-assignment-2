package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;
import bgu.spl.mics.MicroService;

public class MicroServiceChangeStateBroadcast implements Broadcast 
{
    private MicroService service;
    private boolean isEnded;
    
    public MicroServiceChangeStateBroadcast(MicroService service, boolean isEnded)
    {
        this.service = service;
        this.isEnded = isEnded;
    }

    public MicroService getService()
    {
        return this.service;
    }

    public boolean getIsEnded()
    {
        return this.isEnded;
    }
}
