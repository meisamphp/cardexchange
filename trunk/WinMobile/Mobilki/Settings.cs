using System;
using System.Linq;
using System.Collections.Generic;
using System.Text;
using System.Collections.Specialized;
using System.IO;
using System.Xml;

namespace Mobilki
{
    class Settings
    {
        private static NameValueCollection settings;
        private static string settingsPath;

        public static double gpsLat = 0.0;
        public static double gpsLon = 0.0;
        public static double gpsAcc = 0.0;
        public static double cellLat = 0.0;
        public static double cellLon = 0.0;
        public static double cellAcc = 0.0;
        public static long time = 0;

        // Static Ctor
        static Settings()
        {
            // Get the path of the settings file.
            settingsPath = Path.GetDirectoryName(
            System.Reflection.Assembly.GetExecutingAssembly().GetName().CodeBase);
            settingsPath += @"\appSettings.xml";

            if (!File.Exists(settingsPath))
                throw new FileNotFoundException(
                                  settingsPath + " could not be found.");

            System.Xml.XmlDocument xdoc = new XmlDocument();
            xdoc.Load(settingsPath);
            XmlElement root = xdoc.DocumentElement;
            System.Xml.XmlNodeList nodeList = xdoc.GetElementsByTagName("add"); //root.ChildNodes.Item(0).ChildNodes;

            // Add settings to the NameValueCollection.

            settings = new NameValueCollection();
            settings.Add("Name", nodeList.Item(0).Attributes["value"].Value);
            settings.Add("PhoneNumber", nodeList.Item(1).Attributes["value"].Value);
        }

        public static string Name
        {
            get { return settings.Get("Name"); }
            set { settings.Set("Name", value); }
        }

        public static string PhoneNumber
        {
            get { return settings.Get("PhoneNumber"); }
            set { settings.Set("PhoneNumber", value); }
        }

        public static void setLocation(Location location)
        {
            cellAcc = location.accuracy;
            cellLat = location.latitude;
            cellLon = location.longitude;
        }

        public static void setTime()
        {
            time = ByteUtils.getProperTime(DateTime.Now);
        }



        public static void Update()
        {
            XmlTextWriter tw = new XmlTextWriter(settingsPath,
                                               System.Text.UTF8Encoding.UTF8);
            tw.WriteStartDocument();
            tw.WriteStartElement("configuration");
            tw.WriteStartElement("appSettings");

            for (int i = 0; i < settings.Count; ++i)
            {
                tw.WriteStartElement("add");
                tw.WriteStartAttribute("key", string.Empty);
                tw.WriteRaw(settings.GetKey(i));
                tw.WriteEndAttribute();

                tw.WriteStartAttribute("value", string.Empty);
                tw.WriteRaw(settings.Get(i));
                tw.WriteEndAttribute();
                tw.WriteEndElement();
            }

            tw.WriteEndElement();
            tw.WriteEndElement();

            tw.Close();
        }

        // ===========================================================

        public static byte[] ToByteArray()
        {
            int messageType = MsgType.CLIENT_DATA;
 
            byte[] nameBytes = ByteUtils.writeUtfString(Settings.Name);
            byte[] phoneBytes = ByteUtils.writeUtfString(Settings.PhoneNumber);

            int dataLen = ByteUtils.LONG_SIZE // time
                + ByteUtils.DOUBLE_SIZE * 3 // gps
                + ByteUtils.DOUBLE_SIZE * 3 // cellId
                + nameBytes.Length
                + phoneBytes.Length;

            byte[] buffer = new byte[dataLen + ByteUtils.INT_SIZE * 2]; // data length + message type

            int offset = 0;
            offset = ByteUtils.writeIntBytes(messageType, buffer, offset);
            offset = ByteUtils.writeIntBytes(dataLen, buffer, offset);
            offset = ByteUtils.writeLongBytes(time, buffer, offset);
            offset = ByteUtils.writeDoubleBytes(gpsLat, buffer, offset);
            offset = ByteUtils.writeDoubleBytes(gpsLon, buffer, offset);
            offset = ByteUtils.writeDoubleBytes(gpsAcc, buffer, offset);
            offset = ByteUtils.writeDoubleBytes(cellLat, buffer, offset);
            offset = ByteUtils.writeDoubleBytes(cellLon, buffer, offset);
            offset = ByteUtils.writeDoubleBytes(cellAcc, buffer, offset);
            Buffer.BlockCopy(nameBytes, 0, buffer, offset, nameBytes.Length);
            offset += nameBytes.Length;
            Buffer.BlockCopy(phoneBytes, 0, buffer, offset, phoneBytes.Length);
            offset += phoneBytes.Length;

            return buffer;
        }


    }
}
