package com.indentationerror.lightyear.client;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import com.indentationerror.json.*;

class ConnectButtonListener implements ActionListener {
    private GUI gui;

    ConnectButtonListener(GUI gui) {
        this.gui = gui;
    }

    public void actionPerformed(ActionEvent e){
        gui.connectToServer(gui.connectionHost.getText());
    }
}

public class GUI {

    BufferedImage bufferedImage;
    JButton connectButton;
    JTextField connectionHost;
    int networkBaudRate;
    int horizontalResolution;
    int verticalResolution;
    int fps;
    DataReceiveServer drs;
    Thread drsThread;
    Screen screen;

    public GUI() {
        this.networkBaudRate = 1024 * 1024 * 128; // 128 mbps
        this.horizontalResolution = 1280;
        this.verticalResolution = 720;
        this.fps = 30;

        JFrame frame = new JFrame("Lightyear");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JToolBar toolbar = new JToolBar();
        toolbar.setRollover(true);

        this.connectionHost = new JTextField("192.168.122.109:7482");
        toolbar.add(connectionHost);

        toolbar.addSeparator();

        this.connectButton = new JButton("Connect");
        this.connectButton.addActionListener(new ConnectButtonListener(this));
        toolbar.add(this.connectButton);


        Container contentPane = frame.getContentPane();
        contentPane.add(toolbar, BorderLayout.NORTH);

        redrawImage();

        JLabel imageLabel = new JLabel(new ImageIcon(this.bufferedImage));
        contentPane.add(imageLabel, BorderLayout.CENTER);

        frame.setSize(600, 200);
        frame.setVisible(true);
    }

    public void connectToServer(String hostPort) {
        //System.out.println("Connecting to " + hostPort);
        String host = hostPort.split(":")[0];
        int port = Integer.parseInt(hostPort.split(":")[1]);
        System.out.print("Connecting to " + host);
        System.out.println(" on " + port);
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port),100);
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("op","initConnection");
            jsonObject.put("fps",this.fps);
            jsonObject.put("baudRate",this.networkBaudRate);
            jsonObject.put("host","192.168.1.8");
            jsonObject.put("port",3254);
            byte[] outputData = jsonObject.stringify(4).getBytes();
            outputStream.write(outputData);
            byte[] inputData = new byte[512];
            inputStream.read(inputData);
            String inputJson = (new String(inputData)).replaceAll("\r", "");
            System.out.println(inputJson);
            jsonObject = new JSONObject(inputJson);
            switch (jsonObject.get("op").asString()) {
                case "initConnection":
                    System.out.println("OK, starting recieve server");
                    this.horizontalResolution = jsonObject.get("resolution").get(0).asInt();
                    this.verticalResolution = jsonObject.get("resolution").get(1).asInt();
                    this.screen = new Screen(this.horizontalResolution,this.verticalResolution,this.fps);
                    this.drs = new DataReceiveServer(3254);
                    this.drs.setOutputScreen(this.screen);
                    this.drsThread = new Thread(this.drs);
                    this.drsThread.start();
                    //this.drs.setOutputScreen(this.screen);
                    //this.drs.run();
                    break;
                default:
                    System.err.println("ERROR CONNECTING");
                    break;
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void redrawImage() {
        int width = 1280;
        int height = 720;
        this.bufferedImage = new BufferedImage(width, height,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = (Graphics2D) this.bufferedImage.getGraphics();
    }
}
