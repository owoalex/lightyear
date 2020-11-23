using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace lightyear_server_windows
{
    class ImageFrameComponent
    {
        int width;
        int height;
        int subsampling;
        public byte[] image;
        public ImageFrameComponent(int width, int height, int subsampling) 
        {
            this.width = width;
            this.height = height;
            this.subsampling = subsampling;
            this.image = new byte[width * height];
        }
    }
}
