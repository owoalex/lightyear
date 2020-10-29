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
    public partial class Setup : Form
    {
        bool enableOutput;

        FrameServer frameServer;
        public Setup()
        {
            InitializeComponent();
            SetupServer setupServer = new SetupServer();
            setupServer.StartServer(this);
        }

        public void StartFrameServer(int networkBaudRate, int fps, String remoteHost, int remotePort)
        {
            this.frameServer = new FrameServer(networkBaudRate, fps, remoteHost, remotePort);
            frameServer.StartServerLoop();
        }

        public void StartLoop()
        {
            enableOutput = true;
        }

        private void button1_Click(object sender, EventArgs e)
        {
            button1.Text = "Test";
            //SetupServer setupServer = new SetupServer();
            //setupServer.StartServer(this);
            
        }


        private void notifyIcon1_MouseDoubleClick(object sender, MouseEventArgs e)
        {

        }
    }
}
