using System;
using System.Linq;
using System.Collections.Generic;
using System.Text;
using System.Collections;

namespace Mobilki
{
    public class PairList : Hashtable
    {
        public void fromByteArray(byte[] data)
        {
            int intSize = 4;
            byte[] tmp = new byte[intSize];
            int pos = 0;
            
            Buffer.BlockCopy(data, pos, tmp, 0, intSize);
            Array.Reverse(tmp);
            int n = BitConverter.ToInt32(tmp, 0);
            pos += intSize;

            for (int i = 0; i < n; i++)
            {
                Buffer.BlockCopy(data, pos, tmp, 0, intSize);
                Array.Reverse(tmp);
                int id = BitConverter.ToInt32(tmp, 0);
                pos += intSize;

                string name = BitConverter.ToString(data, pos);
                pos += name.Length;

                Add(id, name);
            }
        }
	
        public byte[] toByteArray()
        {
            int n = Count;
            byte[] t = BitConverter.GetBytes(n);
            foreach (KeyValuePair<int, string> p in this)
            {
                int k = (int)p.Key;
                string s = (string)p.Value;
                byte[] kb = BitConverter.GetBytes(k);
                byte[] sb = (new UTF8Encoding()).GetBytes(s);

                byte[] m = new byte[t.Length + kb.Length + sb.Length];
                Buffer.BlockCopy(t, 0, m, 0, t.Length);
                int o = t.Length;
                Buffer.BlockCopy(kb, 0, m, o, kb.Length);
                o += kb.Length;
                Buffer.BlockCopy(sb, 0, m, o, sb.Length);
                t = m;
            }

            return t;
		}
        
    }
}
