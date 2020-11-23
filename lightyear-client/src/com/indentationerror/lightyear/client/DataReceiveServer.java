package com.indentationerror.lightyear.client;

import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;

public class DataReceiveServer implements Runnable {
    private DatagramSocket socket;
    public byte[] dataBuffer; // using a queue is actually smart here
    public ArrayList<NetworkFrameInfo> networkFrameIndexes;
    public int dataBufferTop;
    private Screen outputScreen;

    public DataReceiveServer(int port) {
        this.dataBuffer = new byte[1024 * 1024 * 32];
        this.dataBufferTop = 0;
        this.networkFrameIndexes = new ArrayList<>();
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void setOutputScreen(Screen outputScreen) {
        this.outputScreen = outputScreen;
    }

    public int unsignedByteToInt(byte byteIn) {
        //if (byteIn < 0) {
            //System.out.println("invert");
        return byteIn & 0xFF;
        //} else {
            //System.out.println("noinvert");
            //return byteIn;
        //}
    }

    public int unsignedShortBytesToInt(byte byteIn0,byte byteIn1) {
        //if (shortIn < 0) {
            //System.out.println("invert");
        return ((byteIn0 & 0xFF) * 256) + (byteIn1 & 0xFF);
        //} else {
            //System.out.println("noinvert");
        //    return shortIn;
        //}
    }

    public long unsignedIntBytesToLong(byte byteIn0,byte byteIn1,byte byteIn2,byte byteIn3) {
        return ((byteIn0 & 0xFF) * (long) 16777216) + ((byteIn1 & 0xFF) * (long) 65536) + ((byteIn2 & 0xFF) * (long) 256) + (long) (byteIn3 & 0xFF);
    }

    public NetworkFrameInfo getNetFrameInfo(int frameId) {
        for (int i=0; i<this.networkFrameIndexes.size(); i++) {
            if (this.networkFrameIndexes.get(i).getId() == frameId) {
                return this.networkFrameIndexes.get(i);
            }
        }
        return null;
    }

    @Override
    public void run() {
        boolean running = true;
        byte[] buffer = new byte[528];

        while (running) {

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            byte[] packetData = packet.getData();
            if (unsignedByteToInt(packetData[0]) == 0x81 && unsignedByteToInt(packetData[1]) == 0x55) {
                //System.out.println("GOT PACKET bruh");
                int packetIndex = unsignedShortBytesToInt(packetData[2],packetData[3]);
                long timestamp = unsignedIntBytesToLong(packetData[4],packetData[5],packetData[6],packetData[7]);
                long ssrc = unsignedIntBytesToLong(packetData[8],packetData[9],packetData[10],packetData[11]);
                //long csrc = unsignedIntBytesToLong(packetData[12],packetData[13],packetData[14],packetData[15]);
                int frameId = unsignedShortBytesToInt(packetData[12],packetData[13]);
                int framePackets = unsignedShortBytesToInt(packetData[14],packetData[15]);
                //System.out.println("packet       " + (packetIndex + 1) + " / " + framePackets);
                //System.out.println("timestamp    " + timestamp);
                //System.out.println("ssrc         " + ssrc);
                //System.out.println("csrc " + csrc);
                //System.out.println("frame id     " + frameId);

                NetworkFrameInfo nfi = getNetFrameInfo(frameId);
                int depositLocation = 0;
                if (nfi == null) {
                    nfi = new NetworkFrameInfo(frameId, framePackets, this.dataBufferTop, timestamp);
                    this.networkFrameIndexes.add(nfi);
                    this.dataBufferTop = (this.dataBufferTop + framePackets * 512) % this.dataBuffer.length;
                    //System.out.println("timestamp    " + timestamp);
                    //System.out.println("ssrc         " + ssrc);
                    //System.out.println("frame id     " + frameId);
                }
                depositLocation = (nfi.getLocation() + (packetIndex * 512)) % this.dataBuffer.length;
                for (int i=0; i<512; i++) {
                    this.dataBuffer[depositLocation + i] = packetData[i + 16];
                }
                nfi.recordNewPacket();
                if (nfi.getPacketTotal() == nfi.getPacketsReceived()) {
                    byte[] netFrameFinal = new byte[nfi.getPacketTotal() * 512];
                    for (int i=0; i<(nfi.getPacketTotal() * 512); i++) {
                        netFrameFinal[i] = this.dataBuffer[(nfi.getLocation() + i) % this.dataBuffer.length];
                    }

                    if (unsignedByteToInt(netFrameFinal[0]) == 0x6A &&
                            unsignedByteToInt(netFrameFinal[1]) == 0x70 &&
                            unsignedByteToInt(netFrameFinal[2]) == 0x65 &&
                            unsignedByteToInt(netFrameFinal[3]) == 0x67) {
                        byte[] jpegData = new byte[netFrameFinal.length - 4];
                        for (int i=0; i<(netFrameFinal.length - 4); i++) {
                            jpegData[i] = netFrameFinal[i+4];
                        }
                        System.out.println("JPEG FRAME");
                        outputScreen.drawJpeg(jpegData);
                    } else if (unsignedByteToInt(netFrameFinal[0]) == 0x64 &&
                            unsignedByteToInt(netFrameFinal[1]) == 0x64 &&
                            unsignedByteToInt(netFrameFinal[2]) == 0x63 &&
                            unsignedByteToInt(netFrameFinal[3]) == 0x74) {
                        byte[] ycbcrData = new byte[netFrameFinal.length - 4];
                        for (int i=0; i<(netFrameFinal.length - 4); i++) {
                            ycbcrData[i] = netFrameFinal[i+4];
                        }
                        System.out.println("YCBCR FRAME");
                        outputScreen.drawDiff(ycbcrData);
                    } else {
                        System.out.println("NO FRAME");
                        System.out.println(unsignedByteToInt(netFrameFinal[0]));
                        System.out.println(unsignedByteToInt(netFrameFinal[1]));
                        System.out.println(unsignedByteToInt(netFrameFinal[2]));
                        System.out.println(unsignedByteToInt(netFrameFinal[3]));
                    }
                }
            } else {
            }

            //InetAddress address = packet.getAddress();
            //int port = packet.getPort();
            //String received = new String(, 0, packet.getLength());
            //dataFrames.add(packet.getData());


            //if (received.equals("end")) {
            //    running = false;
            //    continue;
            //}
            //System.out.println("GOT PACKET");

            //socket.send(packet);
        }

        socket.close();
    }
}
