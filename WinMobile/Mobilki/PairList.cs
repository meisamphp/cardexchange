using System;
using System.Linq;
using System.Collections.Generic;
using System.Text;
using System.Collections;

namespace Mobilki
{
    public class PairList : Hashtable
    {
        // convert the byte data to a map of partner names and IDs
        public void fromByteArray(byte[] data)
        {
            int pairCount = ByteUtils.extractInt(data, 0);
            int pos = ByteUtils.INT_SIZE;

            for (int i = 0; i < pairCount; i++)
            {
                int id = ByteUtils.extractInt(data, pos);
                pos += ByteUtils.INT_SIZE;

                string name = ByteUtils.readUtfString(data, pos);
                ushort nameLen = ByteUtils.extractShort(data, pos);
                pos += nameLen + ByteUtils.SHORT_SIZE; // string length + string

                Add(id, name);
            }
        }
    }
}
