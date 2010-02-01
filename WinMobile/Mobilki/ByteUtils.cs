using System;
using System.Linq;
using System.Collections.Generic;
using System.Text;

namespace Mobilki
{
    class ByteUtils
    {
        public const int SHORT_SIZE = sizeof(ushort);
        public const int INT_SIZE = sizeof(int);
        public const int LONG_SIZE = sizeof(long);
        public const int DOUBLE_SIZE = sizeof(double);

        public static int extractInt(byte[] buff, int pos)
        {
            byte[] tmp = new byte[INT_SIZE];
            Buffer.BlockCopy(buff, pos, tmp, 0, INT_SIZE);
            Array.Reverse(tmp);
            return BitConverter.ToInt32(tmp, 0);
        }

        public static ushort extractShort(byte[] buff, int pos)
        {
            byte[] lenBytes = new byte[SHORT_SIZE];
            lenBytes[0] = buff[pos+1];
            lenBytes[1] = buff[pos];
            return (ushort) BitConverter.ToUInt16(lenBytes, 0);
        }

        public static int writeIntBytes(int value, byte[] buffer, int offset)
        {
            byte[] b = BitConverter.GetBytes(value);
            Array.Reverse(b);
            Buffer.BlockCopy(b, 0, buffer, offset, ByteUtils.INT_SIZE);
            return offset + ByteUtils.INT_SIZE;
        }

        public static int writeLongBytes(long value, byte[] buffer, int offset)
        {
            byte[] b = BitConverter.GetBytes(value);
            Array.Reverse(b);
            Buffer.BlockCopy(b, 0, buffer, offset, ByteUtils.LONG_SIZE);
            return offset + ByteUtils.LONG_SIZE;
        }

        public static int writeDoubleBytes(double value, byte[] buffer, int offset)
        {
            byte[] b = BitConverter.GetBytes(value);
            Array.Reverse(b);
            Buffer.BlockCopy(b, 0, buffer, offset, ByteUtils.DOUBLE_SIZE);
            return offset + ByteUtils.DOUBLE_SIZE;
        }

        public static byte[] writeUtfString(string s)
        {
            byte[] ss = (new UTF8Encoding()).GetBytes(s.ToCharArray());
            byte[] result = new byte[ss.Length + SHORT_SIZE];
            byte[] size = BitConverter.GetBytes((ushort)ss.Length);
            Array.Reverse(size);
            Buffer.BlockCopy(size, 0, result, 0, SHORT_SIZE);
            Buffer.BlockCopy(ss, 0, result, SHORT_SIZE, ss.Length);

            return result;
        }

        public static string readUtfString(byte[] b, int offset)
        {
            //Denmark from chicken
            ushort len = extractShort(b, offset);
            return (new UTF8Encoding()).GetString(b, SHORT_SIZE + offset, len);
        }

        public static long getProperTime(DateTime date)
        {
            var result = date.ToUniversalTime() - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
            return (long)(result.TotalSeconds * 1000);
        }
    }
}
