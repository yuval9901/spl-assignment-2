package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

public class TerminatedBroadcast implements Broadcast {

    private final String senderServiceName;

    public TerminatedBroadcast(String senderServiceName) {
        this.senderServiceName = senderServiceName;
    }

    public String getSenderServiceName() {
        return senderServiceName;
    }
}
