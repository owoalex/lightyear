using DesktopDuplication;
using SharpDX;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Windows.Forms;

namespace lightyear_server_windows
{
    class FrameServer
    {
        Random rnd = new Random();
        private DesktopDuplicator desktopDuplicator;
        private UdpClient receivingUdpClient;
        private IPEndPoint remoteIpEndPoint;
        int networkBaudRate;
        int fps;
        uint sessionId;
        String remoteHost;
        int remotePort;
        EncoderParameters encoderParameters;
        ImageCodecInfo imageCodecInfo;
        uint timestamp;
        long startMilliseconds;
        uint lastFrame;
        UdpClient udpSendClient;
        ushort currentNetworkFrameId;

        int frameWidth;
        int frameHeight;
        ImageFrameComponent currentFrameY;
        ImageFrameComponent currentFrameCb;
        ImageFrameComponent currentFrameCr;
        ImageFrameComponent diffFrameY;
        ImageFrameComponent diffFrameCb;
        ImageFrameComponent diffFrameCr;
        ImageFrameComponent remoteFrameY;
        ImageFrameComponent remoteFrameCb;
        ImageFrameComponent remoteFrameCr;

        public FrameServer(int networkBaudRate, int fps, String remoteHost, int remotePort)
        {
            try
            {
                this.desktopDuplicator = new DesktopDuplicator(0);
            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.ToString());
            }
            this.networkBaudRate = networkBaudRate;
            this.fps = fps;
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            this.timestamp = 0;
            this.encoderParameters = new EncoderParameters(1);
            this.encoderParameters.Param[0] = new EncoderParameter(System.Drawing.Imaging.Encoder.Quality,10L);
            this.imageCodecInfo = GetEncoder(ImageFormat.Jpeg);
            this.sessionId = (uint)rnd.NextLong();
            this.startMilliseconds = DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond;
            this.udpSendClient = new UdpClient(this.remoteHost, this.remotePort);
            this.currentNetworkFrameId = 0;
        }

        public ushort GetNetworkFrameId() 
        {
            return currentNetworkFrameId++;
        }
        public void StartServerLoop()
        {
            ThreadStart childRef = new ThreadStart(FrameServerLoop);
            Thread childThread = new Thread(childRef);
            childThread.Start();
            Console.WriteLine("Spawned Child Thread");
        }

        private void FrameServerLoop() 
        {
            double currentTime;
            double nextFrameTime;
            while (true)
            {
                currentTime = (DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond);
                nextFrameTime = currentTime + (1000d / fps);
                //Console.WriteLine("bruh" + );
                this.SendFrame();
                //Console.WriteLine("Sleep");
                while (nextFrameTime > (DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond))
                {

                }
                //Console.WriteLine("Wake");
                //Thread.Sleep(1000 / fps);
            }
        }

        private void udpServer()
        {
            //Creates a UdpClient for reading incoming data.
            receivingUdpClient = new UdpClient(11000);

            //Creates an IPEndPoint to record the IP Address and port number of the sender.
            // The IPEndPoint will allow you to read datagrams sent from any source.
            remoteIpEndPoint = new IPEndPoint(IPAddress.Any, 0);
            try
            {

                // Blocks until a message returns on this socket from a remote host.
                Byte[] receiveBytes = receivingUdpClient.Receive(ref remoteIpEndPoint);

                string returnData = Encoding.ASCII.GetString(receiveBytes);

                Console.WriteLine("This is the message you received " +
                                          returnData.ToString());
                Console.WriteLine("This message was sent from " +
                                            remoteIpEndPoint.Address.ToString() +
                                            " on their port number " +
                                            remoteIpEndPoint.Port.ToString());
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.ToString());
            }
        }

        private static ImageCodecInfo GetEncoder(ImageFormat format)
        {
            ImageCodecInfo[] codecs = ImageCodecInfo.GetImageDecoders();

            foreach (ImageCodecInfo codec in codecs)
            {
                if (codec.FormatID == format.Guid)
                {
                    return codec;
                }
            }

            return null;
        }

        public void SendFrame()
        {
            DesktopFrame frame = null;
            //Debug.WriteLine("Frame Time" + (timestamp % ((uint)(1000 / fps))));
            timestamp = (uint) ((DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond) - startMilliseconds);
            //Debug.WriteLine("Frame Time ST " + timestamp);
            if ((timestamp - lastFrame) > (1000 / fps))
            {
                lastFrame = timestamp;
                try
                {
                    frame = desktopDuplicator.GetLatestFrame();
                }
                catch
                {
                    desktopDuplicator = new DesktopDuplicator(0);
                    frame = null;
                }

                if (frame != null)
                {
                    //labelCursor.Location = frame.CursorLocation;
                    //labelCursor.Visible = frame.CursorVisible;

                    //Debug.WriteLine("--------------------------------------------------------");
                    //foreach (var moved in frame.MovedRegions)
                    //{
                    //   Debug.WriteLine(String.Format("Moved: {0} -> {1}", moved.Source, moved.Destination));
                    //moved.Destination.Location;
                    //moved.Destination.Size;
                    bool updatedRegions = true;
                    //}
                    foreach (var updated in frame.UpdatedRegions)
                    {
                        //Debug.WriteLine(String.Format("Updated: {0}", updated.ToString()));
                        updatedRegions = true;
                        //UpdatedRegion.Location = updated.Location;
                        //UpdatedRegion.Size = updated.Size;
                    }
                    //pictureBox1.Image = frame.DesktopImage;

                    if (updatedRegions)
                    {
                        bool exportJpeg = false;
                        bool exportDiff = false;

                        if (currentFrameY == null)
                        {
                            this.frameWidth = frame.DesktopImage.Width;
                            this.frameHeight = frame.DesktopImage.Height;
                            currentFrameY = new ImageFrameComponent(this.frameWidth, this.frameHeight, 1);
                            currentFrameCb = new ImageFrameComponent(this.frameWidth, this.frameHeight, 1);
                            currentFrameCr = new ImageFrameComponent(this.frameWidth, this.frameHeight, 1);
                            diffFrameY = new ImageFrameComponent(this.frameWidth, this.frameHeight, 1);
                            diffFrameCb = new ImageFrameComponent(this.frameWidth, this.frameHeight, 1);
                            diffFrameCr = new ImageFrameComponent(this.frameWidth, this.frameHeight, 1);
                            remoteFrameY = new ImageFrameComponent(this.frameWidth, this.frameHeight, 1);
                            remoteFrameCb = new ImageFrameComponent(this.frameWidth, this.frameHeight, 1);
                            remoteFrameCr = new ImageFrameComponent(this.frameWidth, this.frameHeight, 1);

                            exportJpeg = true;
                            exportDiff = false;
                        }
                        else 
                        {
                            exportJpeg = false;
                            exportDiff = true;
                        }

                        if (exportDiff)
                        {
                            //byte[] pngData = null;
                            //MemoryStream stream = new MemoryStream();

                            this.frameWidth = frame.DesktopImage.Width;
                            this.frameHeight = frame.DesktopImage.Height;
                            int blocksWidth = (int) Math.Ceiling(this.frameWidth / 8.0);
                            int blocksHeight = (int) Math.Ceiling(this.frameHeight / 8.0);

                            int timestampView = (int)((DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond) - startMilliseconds);
                            //Debug.WriteLine("Frame Time Extract Frame RGB " + timestampView);

                            System.Drawing.Rectangle rect = new System.Drawing.Rectangle(0, 0, frame.DesktopImage.Width, frame.DesktopImage.Height);
                            System.Drawing.Imaging.BitmapData bmpData = frame.DesktopImage.LockBits(rect, System.Drawing.Imaging.ImageLockMode.ReadOnly, frame.DesktopImage.PixelFormat);

                            // Get the address of the first line.
                            IntPtr ptr = bmpData.Scan0;

                            // Declare an array to hold the bytes of the bitmap.
                            int frameRgbBytesCount = bmpData.Stride * frame.DesktopImage.Height;
                            byte[] rgbValues = new byte[frameRgbBytesCount];

                            // Copy the RGB values into the array.
                            System.Runtime.InteropServices.Marshal.Copy(ptr, rgbValues, 0, frameRgbBytesCount); 
                            frame.DesktopImage.UnlockBits(bmpData);

                            //timestampView = (int)((DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond) - startMilliseconds);
                            //Debug.WriteLine("Frame Time BuildDiffsStart " + timestampView);
                            int i = 0;
                            int j = 0;
                            for (int x = 0; x < this.frameWidth; x++)
                            {
                                for (int y = 0; y < this.frameHeight; y++)
                                {
                                    i = (y * frameWidth) + x;
                                    j = ((y * frameWidth) + x) * 4;
                                    //currentPixel = frame.DesktopImage.GetPixel(x,y);
                                    this.remoteFrameY.image[i] = (byte)(this.remoteFrameY.image[i] + this.diffFrameY.image[i]);
                                    this.remoteFrameCb.image[i] = (byte)(this.remoteFrameCb.image[i] + this.diffFrameCb.image[i]);
                                    this.remoteFrameCr.image[i] = (byte)(this.remoteFrameCr.image[i] + this.diffFrameCr.image[i]);
                                    //this.currentFrameY.image[(y * frameWidth) + x] = (byte)((currentPixel.R / 256.0 * 65.481) + (currentPixel.G / 256.0 * 128.553) + (currentPixel.B / 256.0 * 24.966) + 16);
                                    //this.currentFrameCb.image[(y * frameWidth) + x] = (byte)(-(currentPixel.R / 256.0 * 37.797) - (currentPixel.G / 256.0 * 74.203) + (currentPixel.B / 256.0 * 112.0) + 128);
                                    //this.currentFrameCr.image[(y * frameWidth) + x] = (byte)((currentPixel.R / 256.0 * 112.0) - (currentPixel.G / 256.0 * 93.786) - (currentPixel.B / 256.0 * 18.214) + 128);

                                    this.currentFrameY.image[i] = (byte)((rgbValues[j] * 65 / 256) + (rgbValues[j+1] * 129 / 256) + (rgbValues[j+2] * 25 / 256) + 16);
                                    this.currentFrameCb.image[i] = (byte)(-(rgbValues[j] * 38 / 256) - (rgbValues[j+1] * 74 / 256) + (rgbValues[j+2] * 112 / 256) + 128);
                                    this.currentFrameCr.image[i] = (byte)((rgbValues[j] * 112 / 256) - (rgbValues[j+1] * 94 / 256) - (rgbValues[j+2] * 18 / 256) + 128);

                                    this.diffFrameY.image[i] = (byte)(this.currentFrameY.image[i] - this.remoteFrameY.image[i]);
                                    this.diffFrameCb.image[i] = (byte)(this.currentFrameCb.image[i] - this.remoteFrameCb.image[i]);
                                    this.diffFrameCr.image[i] = (byte)(this.currentFrameCr.image[i] - this.remoteFrameCr.image[i]);
                                }
                            }
                            //timestampView = (int)((DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond) - startMilliseconds);
                            //Debug.WriteLine("Frame Time DDCT Convert " + timestampView);


                            //this.currentFrameY = 

                            //pngData = stream.ToArray();
                            int DDCT_HEADER_LENGTH = 8;
                            int DDCT_BLOCK_SIZE = 8;
                            byte[] networkFrameData = new byte[DDCT_HEADER_LENGTH + this.currentFrameY.image.Length + (this.currentFrameY.image.Length/8)];
                            networkFrameData[0] = 0x64;//d;
                            networkFrameData[1] = 0x64;//d;
                            networkFrameData[2] = 0x63;//c;
                            networkFrameData[3] = 0x74;//t;
                            networkFrameData[4] = 0x01;//ddct protocol v1;
                            networkFrameData[5] = 0x01;//channel Y;
                            networkFrameData[6] = 0x01;//subsampling level (1:1);
                            networkFrameData[7] = 0x00;//reserved;
                            //for (int i = 0; i < this.currentFrameY.image.Length; i++)
                            //{
                            //    networkFrameData[i + DDCT_HEADER_LENGTH] = (byte) this.diffFrameY.image[i];
                            //}
                            int byteCounter = 8;
                            int runningLiteralsCountPosition = 0;
                            
                            for (int blkY = 0; blkY < (((this.frameHeight - 1) / DDCT_BLOCK_SIZE) + 1); blkY++)
                            {
                                for (int blkX = 0; blkX < (((this.frameWidth - 1) / DDCT_BLOCK_SIZE) + 1); blkX++)
                                {
                                    runningLiteralsCountPosition = byteCounter;
                                    networkFrameData[runningLiteralsCountPosition] = 255;
                                    for (int yOffset = 0; yOffset < DDCT_BLOCK_SIZE; yOffset++)
                                    {
                                        for (int xOffset = 0; xOffset < DDCT_BLOCK_SIZE; xOffset++)
                                        {
                                            if (this.diffFrameY.image[(((blkY * DDCT_BLOCK_SIZE) + yOffset) * frameWidth) + ((blkX * DDCT_BLOCK_SIZE) + xOffset)] != 0)
                                            {
                                                if ((networkFrameData[runningLiteralsCountPosition] >= 0) && (networkFrameData[runningLiteralsCountPosition] <= 63))
                                                {
                                                    networkFrameData[runningLiteralsCountPosition]++;
                                                }
                                                else 
                                                {
                                                    runningLiteralsCountPosition = byteCounter;
                                                    networkFrameData[runningLiteralsCountPosition] = 0;
                                                    byteCounter++;
                                                }
                                                networkFrameData[byteCounter] = this.diffFrameY.image[(((blkY * DDCT_BLOCK_SIZE) + yOffset) * frameWidth) + ((blkX * DDCT_BLOCK_SIZE) + xOffset)];
                                                byteCounter++;
                                            }
                                            else
                                            {
                                                if ((networkFrameData[runningLiteralsCountPosition] >= 64) && (networkFrameData[runningLiteralsCountPosition] <= 128))
                                                {
                                                    networkFrameData[runningLiteralsCountPosition]++;
                                                }
                                                else
                                                {
                                                    runningLiteralsCountPosition = byteCounter;
                                                    networkFrameData[runningLiteralsCountPosition] = 64;
                                                    byteCounter++;
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            //timestampView = (int)((DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond) - startMilliseconds);
                            //Debug.WriteLine("Frame Time SendStart " + timestampView);
                            NetworkFrame netFrame = new NetworkFrame(networkFrameData, byteCounter, GetNetworkFrameId(), this.timestamp);
                            netFrame.SendFrame(this.udpSendClient, this.sessionId);
                            //timestampView = (int)((DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond) - startMilliseconds);
                            //Debug.WriteLine("Frame Time SendEnd " + timestampView);
                        }

                        if (exportJpeg)
                        {
                            byte[] jpegData = null;
                            MemoryStream stream = new MemoryStream();
                            frame.DesktopImage.Save(stream, imageCodecInfo, encoderParameters);
                            jpegData = stream.ToArray();
                            byte[] networkFrameData = new byte[jpegData.Length + 4];
                            networkFrameData[0] = 0x6A;//j
                            networkFrameData[1] = 0x70;//p
                            networkFrameData[2] = 0x65;//e
                            networkFrameData[3] = 0x67;//g
                            //File.WriteAllBytes("foo.jpeg", result);
                            for (int i = 0; i < (jpegData.Length); i++)
                            {
                                networkFrameData[i + 4] = jpegData[i];
                            }
                            NetworkFrame netFrame = new NetworkFrame(networkFrameData, GetNetworkFrameId(), this.timestamp);
                            netFrame.SendFrame(this.udpSendClient, this.sessionId);
                        }
                    }
                }
            }
            //timestamp = (uint)((DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond) - startMilliseconds);
            //Debug.WriteLine("Frame Time ED " + timestamp);
        }
    }
}
