using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Threading.Tasks;
using System.Text;

namespace lightyear_server_windows
{
    class SetupServer
    {
        Setup setupForm;
        public void StartServer(Setup setupForm)
        {
            this.setupForm = setupForm;
            new Task(Listen).Start();
        }
        public void Listen()
        {
            Debug.WriteLine("Starting setup server... ");
            TcpListener server = null;
            try
            {
                // Set the TcpListener on port 13000.
                Int32 port = 7482;
                //IPAddress localAddr = IPAddress.Parse("127.0.0.1");

                // TcpListener server = new TcpListener(port);
                server = new TcpListener(IPAddress.Any, port);

                // Start listening for client requests.
                server.Start();

                // Buffer for reading data
                Byte[] bytes = new Byte[256];
                String data = null;

                // Enter the listening loop.
                while (true)
                {
                    Debug.WriteLine("Waiting for a connection... ");

                    // Perform a blocking call to accept requests.
                    // You could also use server.AcceptSocket() here.
                    TcpClient client = server.AcceptTcpClient();
                    Debug.WriteLine("Connected!");

                    data = null;

                    // Get a stream object for reading and writing
                    NetworkStream stream = client.GetStream();

                    int i;

                    // Loop to receive all the data sent by the client.
                    while ((i = stream.Read(bytes, 0, bytes.Length)) != 0)
                    {
                        // Translate data bytes to a ASCII string.
                        data = System.Text.Encoding.UTF8.GetString(bytes, 0, i);
                        Debug.WriteLine("Received: ");
                        Debug.WriteLine(data);

                        JObject jsonObject = (JObject)JsonConvert.DeserializeObject(data);
                        JObject jsonReturnObject = new JObject();
                        byte[] returnMessage;

                        switch ((string)jsonObject["op"])
                        {
                            case "initConnection":
                                this.setupForm.StartFrameServer((int) jsonObject["baudRate"], (int) jsonObject["fps"], (string) jsonObject["host"], (int) jsonObject["port"]);
                                this.setupForm.StartLoop();

                                jsonReturnObject["op"] = "initConnection";
                                jsonReturnObject["resolution"] = new JArray();
                                JArray returnResolution = (JArray) jsonReturnObject["resolution"];
                                returnResolution.Add(1920);
                                returnResolution.Add(1080);
                                returnMessage = System.Text.Encoding.UTF8.GetBytes(jsonReturnObject.ToString());
                                break;
                            default:
                                jsonReturnObject["op"] = "error";
                                jsonReturnObject["error"] = "invalidOperation";
                                returnMessage = System.Text.Encoding.UTF8.GetBytes(jsonReturnObject.ToString());
                                break;
                        }

                        stream.Write(returnMessage, 0, returnMessage.Length);
                        //Debug.WriteLine("Sent: {0}", data);
                    }

                    // Shutdown and end connection
                    client.Close();
                }
            }
            catch (SocketException ex)
            {
                Debug.WriteLine("SocketException: {0}", ex);
            }
            catch (IOException ex)
            {
                Debug.WriteLine("IOException: {0}", ex);
            }
            finally
            {
                // Stop listening for new clients.
                server.Stop();
            }

            Debug.WriteLine("Server closed");
        }
    }
}
