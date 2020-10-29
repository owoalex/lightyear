package com.indentationerror.lightyear.client;

public class NetworkFrameInfo {
    private int size;
    private int location;
    private long timestamp;
    private int frameId;
    private int packetsReceived;

    public NetworkFrameInfo(int frameId, int packetTotal, int location, long timestamp) {
        this.size = packetTotal;
        this.frameId = frameId;
        this.location = location;
        this.timestamp = timestamp;
        this.packetsReceived = 0;
    }

    public int getId() {
        return frameId;
    }

    public int getPacketTotal() {
        return size;
    }

    public int getLocation() {
        return location;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getPacketsReceived() {
        return packetsReceived;
    }

    public void recordNewPacket() {
        packetsReceived++;
    }
}
