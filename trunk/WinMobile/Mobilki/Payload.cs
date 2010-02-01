using System;
using System.Linq;
using System.Collections.Generic;
using System.Text;

namespace Mobilki
{
    class Payload
    {
        public string name;
        public string phoneNumber;

        public void fromByteArray(byte[] data)
        {
            name = ByteUtils.readUtfString(data, 0);
            ushort len = ByteUtils.extractShort(data, 0);
            phoneNumber = ByteUtils.readUtfString(data, len + ByteUtils.SHORT_SIZE);
	    }   
    }
}
