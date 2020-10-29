package com.indentationerror.lightyear.client;


import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;
import com.indentationerror.json.*;

class ConnectButtonListener implements ActionListener {
    private GUI gui;

    ConnectButtonListener(GUI gui) {
        this.gui = gui;
    }

    public void actionPerformed(ActionEvent e){
        gui.networkBaudRate = (Integer.parseInt(gui.connectionMbps.getText()) * 1024);
        gui.fps = Integer.parseInt(gui.connectionFps.getText());
        gui.connectToServer(gui.connectionHost.getText(), Integer.parseInt(gui.connectionPort.getText()));
    }
}

public class GUI {

    BufferedImage bufferedImage;
    JButton connectButton;
    JTextField connectionHost;
    JTextField connectionPort;
    JTextField connectionFps;
    JTextField connectionMbps;
    int networkBaudRate;
    int horizontalResolution;
    int verticalResolution;
    int fps;
    Font font;
    DataReceiveServer drs;
    Thread drsThread;
    Screen screen;

    public GUI() {
        this.networkBaudRate = 32 * 1024 * 1024; // 128 mbps
        this.horizontalResolution = 1280;
        this.verticalResolution = 720;
        this.fps = 30;

        Insets textPanelMargins = new Insets(3,6,4,6);

        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");

        URL fontUrl = ClassLoader.getSystemClassLoader().getResource("com/indentationerror/lightyear/assets/Sen-Bold.ttf");
        try {
            File fontFile = Paths.get(fontUrl.toURI()).toFile();
            this.font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            this.font = this.font.deriveFont(15f);
        } catch (FontFormatException | IOException | URISyntaxException | NullPointerException e) {
            e.printStackTrace();
            this.font = new Font("Serif", Font.PLAIN, 12);
        }
        //this.font = new Font("Serif", Font.PLAIN, 12);

        JFrame frame = new JFrame("Lightyear Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        URL iconUrl = ClassLoader.getSystemClassLoader().getResource("com/indentationerror/lightyear/assets/icon.png");
        ImageIcon ico = new ImageIcon(iconUrl);
        frame.setIconImage(ico.getImage());

        //JToolBar toolbar = new JToolBar();
        //toolbar.setRollover(true);

        JLabel connectionFpsLabel = new JLabel("Maximum FPS");
        connectionFpsLabel.setFont(this.font);
        this.connectionFps = new JTextField(Integer.toString(this.fps));
        this.connectionFps.setMargin(textPanelMargins);
        this.connectionFps.setFont(this.font);

        JLabel connectionMbpsLabel = new JLabel("Target kbps");
        connectionMbpsLabel.setFont(this.font);
        this.connectionMbps = new JTextField(Integer.toString(this.networkBaudRate / 1024));
        this.connectionMbps.setMargin(textPanelMargins);
        this.connectionMbps.setFont(this.font);

        JLabel connectionHostLabel = new JLabel(" Host or IP Address");
        connectionHostLabel.setFont(this.font);
        this.connectionHost = new JTextField("192.168.122.109");
        this.connectionHost.setMargin(textPanelMargins);
        this.connectionHost.setFont(this.font);

        JLabel connectionPortLabel = new JLabel("Port");
        connectionPortLabel.setFont(this.font);
        this.connectionPort = new JTextField("7482");//new JTextField("7482");
        this.connectionPort.setMargin(textPanelMargins);
        this.connectionPort.setFont(this.font);

        //toolbar.add(connectionHost);

        //toolbar.addSeparator();

        this.connectButton = new JButton("Connect");
        this.connectButton.setFont(this.font);
        this.connectButton.addActionListener(new ConnectButtonListener(this));
        //toolbar.add(this.connectButton);

        Container contentPane = frame.getContentPane();
        GroupLayout layout = new GroupLayout(contentPane);
        contentPane.setLayout(layout);
        layout.setAutoCreateContainerGaps(true);
        layout.setAutoCreateGaps(true);


        //BASELINE CENTER LEADING TRAILING
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                .addComponent(connectionFpsLabel)
                                .addComponent(connectionMbpsLabel)
                                .addComponent(connectionPortLabel)
                                .addComponent(connectionHostLabel))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(connectionFps)
                                .addComponent(connectionMbps)
                                .addComponent(connectionPort)
                                .addComponent(connectionHost))
                        .addComponent(connectButton)
        );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(connectionFpsLabel)
                                .addComponent(connectionFps))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(connectionMbpsLabel)
                                .addComponent(connectionMbps))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(connectionPortLabel)
                                .addComponent(connectionPort))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(connectionHostLabel)
                                .addComponent(connectionHost)
                                .addComponent(connectButton))
        );
        //contentPane.add(toolbar, BorderLayout.NORTH);

        frame.setSize(500, 300);
        frame.setVisible(true);
    }

    public void connectToServer(String host, int port) {
        //System.out.println("Connecting to " + hostPort);
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
}
