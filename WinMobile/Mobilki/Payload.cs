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
            UTF8Encoding enc = new UTF8Encoding();
            string s = enc.GetString(data, 0, data.Length);
            System.Diagnostics.Debug.WriteLine("string: " + s);
	    }   
	
	    public byte[] toByteArray()
        {
            UTF8Encoding enc = new UTF8Encoding();
            byte[] n = enc.GetBytes(name);
            byte[] p = enc.GetBytes(phoneNumber);
            byte[] s = new byte[n.Length + p.Length];
            Buffer.BlockCopy(n, 0, s, 0, n.Length);
            Buffer.BlockCopy(p, 0, s, n.Length, p.Length);

            return s;
	    }
    }
}
