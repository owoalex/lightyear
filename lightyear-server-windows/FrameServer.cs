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
            this.encoderParameters.Param[0] = new EncoderParameter(System.Drawing.Imaging.Encoder.Quality, 20L);
            this.imageCodecInfo = GetEncoder(ImageFormat.Jpeg);
            this.sessionId = (uint)rnd.NextLong();
            this.startMilliseconds = DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond;
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
            //Debug.WriteLine("Frame Time " + timestamp);
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
                    bool updatedRegions = false;
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
                        byte[] jpegData = null;
                        using (MemoryStream stream = new MemoryStream())
                        {
                            frame.DesktopImage.Save(stream, imageCodecInfo, encoderParameters);
                            jpegData = stream.ToArray();
                        }
                        //File.WriteAllBytes("foo.jpeg", result);

                        NetworkFrame netFrame = new NetworkFrame(jpegData, (ushort) rnd.Next(), this.timestamp);
                        netFrame.SendFrame(this.remoteHost, this.remotePort, this.sessionId);
                    }
                }
            }
        }
    }
}
