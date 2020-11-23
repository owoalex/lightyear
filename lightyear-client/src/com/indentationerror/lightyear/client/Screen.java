package com.indentationerror.lightyear.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

public class Screen implements KeyListener, ActionListener {
    int horizontalResolution;
    int verticalResolution;
    int fps;
    int[] frameYComponent;
    int[] frameCbComponent;
    int[] frameCrComponent;
    BufferedImage bufferedImage;
    JFrame frame;
    Font font;
    static final int CONTROL_KEY_CODE = 17;
    static final int ALT_KEY_CODE = 18;
    static final int SHIFT_KEY_CODE = 16;
    static final int DDCT_BLOCK_SIZE = 8;
    boolean[] keyModifiers;

    public Screen(int horizontalResolution,int verticalResolution,int fps) {
        this.horizontalResolution = horizontalResolution;
        this.verticalResolution = verticalResolution;
        this.fps = fps;

        this.frameYComponent = new int[this.horizontalResolution * this.verticalResolution];
        this.frameCbComponent = new int[this.horizontalResolution * this.verticalResolution];
        this.frameCrComponent = new int[this.horizontalResolution * this.verticalResolution];

        this.frame = new JFrame("Lightyear Viewer");
        this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.frame.addKeyListener(this);

        URL url = ClassLoader.getSystemClassLoader().getResource("com/indentationerror/lightyear/assets/icon.png");
        ImageIcon ico = new ImageIcon(url);
        this.frame.setIconImage(ico.getImage());

        this.keyModifiers = new boolean[256];

        this.bufferedImage = new BufferedImage(horizontalResolution, verticalResolution,BufferedImage.TYPE_INT_RGB);

        Container contentPane = this.frame.getContentPane();
        JLabel imageLabel = new JLabel(new ImageIcon(this.bufferedImage));
        contentPane.add(imageLabel, BorderLayout.CENTER);

        this.frame.setUndecorated(true);
        this.frame.setAlwaysOnTop(true);
        //this.frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.frame.setSize(this.horizontalResolution, this.verticalResolution);
        this.frame.setAutoRequestFocus(true);
        this.frame.setVisible(true);
    }

    public void setFont(Font font){
        this.font = font;
    }

    /** Handle the key typed event from the text field. */
    public void keyTyped(KeyEvent e) {
        //displayInfo(e, "KEY TYPED: ");
    }

    /** Handle the key pressed event from the text field. */
    public void keyPressed(KeyEvent e) {
        //displayInfo(e, "KEY PRESSED: ");
        //System.out.println("KeyDown: " + e.getKeyCode());
        if (e.getKeyCode() > 0 && e.getKeyCode() < 256) {
            this.keyModifiers[e.getKeyCode()] = true;
        }
        if (this.keyModifiers[CONTROL_KEY_CODE] && this.keyModifiers[ALT_KEY_CODE] && (e.getKeyCode() == 71)) {
            System.out.println("Escape key grab!");
            this.frame.setVisible(false);
            this.frame.dispose();

        }
    }

    /** Handle the key released event from the text field. */
    public void keyReleased(KeyEvent e) {
        //displayInfo(e, "KEY RELEASED: ");
        //System.out.println("KeyUp: " + e.getKeyCode());
        if (e.getKeyCode() > 0 && e.getKeyCode() < 256) {
            this.keyModifiers[e.getKeyCode()] = false;
        }
    }

    /** Handle the button click. */
    public void actionPerformed(ActionEvent e) {
        //Clear the text components.
        //displayArea.setText("");
        //typingArea.setText("");

        //Return the focus to the typing area.
        //typingArea.requestFocusInWindow();
    }

    public int unsignedByteToInt(byte byteIn) {
        return byteIn & 0xFF;
    }

    public void drawJpeg(byte[] imageData) {
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


        //Font font = new Font("Serif", Font.PLAIN, 12);
        graphics.setFont(this.font);

        // Draw a string such that its base line is at x, y

        graphics.setColor(new Color(255, 0, 0, 255));
        graphics.drawString("Press Ctrl + Alt + G to release keyboard", 60, 80);

        this.frame.getContentPane().repaint();

        //graphics.setStroke(new BasicStroke(1));
        //graphics.setColor(new Color(255, 0, 255, 255));
        //graphics.drawLine(1,1,50,50);
        //System.out.println("Drawing");
    }

    public void updateFrame() {
        Graphics2D graphics = (Graphics2D) this.bufferedImage.getGraphics();

        int YValue;
        int CbValue;
        int CrValue;
        //int maxYValue = 0;
        for (int x=0; x<this.horizontalResolution; x++) {
            for (int y=0; y<this.verticalResolution; y++) {
                YValue = this.frameYComponent[(y * this.horizontalResolution) + x];
                CbValue = this.frameCbComponent[(y * this.horizontalResolution) + x];
                CrValue = this.frameCrComponent[(y * this.horizontalResolution) + x];
                //if (YValue > maxYValue) {
                //    maxYValue = YValue;
                //}
                int a = 255;
                int r = YValue;
                int g = YValue;
                int b = YValue;
                int color = (a<<24) | (r<<16) | (g<<8) | b;
                this.bufferedImage.setRGB(x,y,color);
            }
        }
        //System.out.println(maxYValue);

        graphics.setFont(this.font);
        graphics.setColor(new Color(255, 0, 0, 255));
        graphics.drawString("Press Ctrl + Alt + G to release keyboard", 60, 80);

        this.frame.getContentPane().repaint();
    }

    public void drawDiff(byte[] imageData) {
        int value;
        int byteIn;
        //int maxYValue = 0;
        int frameIndex = 0;

        int offsetCounter = 0;
        int blkx = 0;
        int blky = 0;
        int xOffset = 0;
        int yOffset = 0;

        int ddctProtocolVersion = unsignedByteToInt(imageData[offsetCounter]);
        offsetCounter++;
        int channelNumber = unsignedByteToInt(imageData[offsetCounter]);
        offsetCounter++;
        int subsamplingLevel = unsignedByteToInt(imageData[offsetCounter]);
        offsetCounter++;
        int reserved = unsignedByteToInt(imageData[offsetCounter]);
        offsetCounter++;

        int literalsToRead = 0;
        // 0 = Function read
        // >0 = Literal read

        while (offsetCounter < imageData.length) {
            byteIn = unsignedByteToInt(imageData[offsetCounter]);
            offsetCounter++;

            if (literalsToRead > 0) {
                frameIndex = (((blky * DDCT_BLOCK_SIZE) + yOffset) * this.horizontalResolution) + (blkx * DDCT_BLOCK_SIZE) + xOffset;
                value = (this.frameYComponent[frameIndex] + byteIn) % 256;
                this.frameYComponent[frameIndex] = value;
                xOffset++;
                literalsToRead--;
            } else {
                if ((byteIn >= 0) && (byteIn <= 63)) {
                    literalsToRead = byteIn + 1;
                } else if ((byteIn >= 64) && (byteIn <= 128)) {
                    for (int i = 0; i < (byteIn - 63); i++) {
                        xOffset++;
                        if (xOffset >= DDCT_BLOCK_SIZE) {
                            yOffset++;
                            xOffset = 0;
                        }
                        if (yOffset >= DDCT_BLOCK_SIZE) {
                            yOffset = 0;
                            blkx++;
                        }
                    }
                }
            }

            if (xOffset >= DDCT_BLOCK_SIZE) {
                yOffset++;
                xOffset = 0;
            }
            if (yOffset >= DDCT_BLOCK_SIZE) {
                yOffset = 0;
                blkx++;
            }
            if (blkx >= (((this.horizontalResolution - 1) / DDCT_BLOCK_SIZE) + 1)) {
                blky++;
                blkx = 0;
            }
            if (blky >= (((this.verticalResolution - 1) / DDCT_BLOCK_SIZE) + 1)) {
                break;
            }
        }
        System.out.println("Updated frame " + offsetCounter + "bytes");
        updateFrame();
    }
}
