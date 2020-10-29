package com.indentationerror.lightyear.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class Screen {
    int horizontalResolution;
    int verticalResolution;
    int fps;
    BufferedImage bufferedImage;
    JFrame frame;

    public Screen(int horizontalResolution,int verticalResolution,int fps) {
        this.horizontalResolution = horizontalResolution;
        this.verticalResolution = verticalResolution;
        this.fps = fps;

        frame = new JFrame("Lightyear Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.bufferedImage = new BufferedImage(horizontalResolution, verticalResolution,BufferedImage.TYPE_INT_RGB);

        Container contentPane = frame.getContentPane();
        JLabel imageLabel = new JLabel(new ImageIcon(this.bufferedImage));
        contentPane.add(imageLabel, BorderLayout.CENTER);

        frame.setSize(this.horizontalResolution, this.verticalResolution);
        frame.setVisible(true);
    }

    public void redrawImage(byte[] imageData) {
        //this.bufferedImage = new BufferedImage(horizontalResolution, verticalResolution,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = (Graphics2D) this.bufferedImage.getGraphics();
        ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
        BufferedImage bImage = null;
        try {
            bImage = ImageIO.read(bis);
        } catch (IOException e) {
            e.printStackTrace();
        }
        graphics.drawImage(bImage,0,0,null);
        this.frame.getContentPane().repaint();
        //graphics.setStroke(new BasicStroke(1));
        //graphics.setColor(new Color(255, 0, 255, 255));
        //graphics.drawLine(1,1,50,50);
        //System.out.println("Drawing");
    }
}
