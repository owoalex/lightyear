using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Sockets;
using System.Text;
using System.Threading.Tasks;

namespace lightyear_server_windows
{
    class NetworkFrame
    {
        private byte[] data;
        private ushort frameId;
        private uint timestamp;
        private int limit;
        public NetworkFrame(byte[] data, ushort frameId, uint timestamp) 
        {
            this.data = data;
            this.frameId = frameId;
            this.timestamp = timestamp;
            this.limit = -1;
        }

        public NetworkFrame(byte[] data, int limit, ushort frameId, uint timestamp)
        {
            this.data = data;
            this.frameId = frameId;
            this.timestamp = timestamp;
            this.limit = limit;
        }

        public void SendFrame(UdpClient udpClient, uint sessionId)
        {
            byte[] headerBytes = new byte[16];
            headerBytes[0] = 0x81;
            headerBytes[1] = 0x55;


            byte[] sendBytes = new byte[528];
            int bytesToSend = this.data.Length;
            if (this.limit >= 0) {
                bytesToSend = this.limit;
            }
            ushort counter = 0;

            byte[] timestampBytes = BitConverter.GetBytes(this.timestamp);
            headerBytes[4] = timestampBytes[3];
            headerBytes[5] = timestampBytes[2];
            headerBytes[6] = timestampBytes[1];
            headerBytes[7] = timestampBytes[0];
            byte[] ssrcBytes = BitConverter.GetBytes(sessionId);
            headerBytes[8] = ssrcBytes[3];
            headerBytes[9] = ssrcBytes[2];
            headerBytes[10] = ssrcBytes[1];
            headerBytes[11] = ssrcBytes[0];
            byte[] csrcP1Bytes = BitConverter.GetBytes(frameId);
            headerBytes[12] = csrcP1Bytes[1];
            headerBytes[13] = csrcP1Bytes[0];
            byte[] csrcP2Bytes = BitConverter.GetBytes((ushort) Math.Ceiling(bytesToSend / 512d));
            headerBytes[14] = csrcP2Bytes[1];
            headerBytes[15] = csrcP2Bytes[0];
            try
            {
                for (int i = 0; (i * 512) < bytesToSend; i++)
                {
                    //byte[] counterBytes = BitConverter.GetBytes(counter);
                    headerBytes[2] = (byte)(counter >> 8); //counterBytes[1]; //(byte)(counter >> 8);
                    headerBytes[3] = (byte)(counter & 255);//counterBytes[0]; //(byte)(counter & 255);

                    int chunkLength = 512;
                    if (((512 * i) + chunkLength) > bytesToSend)
                    {
                        chunkLength = bytesToSend - (512 * i);
                    }
                    for (int j = 0; j < 16; j++) {
                        sendBytes[j] = headerBytes[j];
                    }
                    int chunkOffset = (512 * i);
                    for (int j = 0; j < chunkLength; j++)
                    {
                        sendBytes[j+16] = this.data[j+chunkOffset];
                    }
                    //Array.Copy(headerBytes, 0, sendBytes, 0, 16);
                    //Array.Copy(this.data, (512 * i), sendBytes, 16, chunkLength);
                    udpClient.Send(sendBytes, 16 + chunkLength);
                    counter++;
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.ToString());
            }
        }
    }
}
