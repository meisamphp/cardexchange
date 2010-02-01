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

        public static double gpsLat = 0.0;
        public static double gpsLon = 0.0;
        public static double gpsAcc = 0.0;
        public static double cellLat = 0.0;
        public static double cellLon = 0.0;
        public static double cellAcc = 0.0;
        public static long time = 0;


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

        public static byte[] ToByteArray()
        {
            UTF8Encoding enc = new UTF8Encoding();

            int messageType = 0;
            byte[] m = BitConverter.GetBytes(messageType);
            
            byte[] t = BitConverter.GetBytes(time);
            byte[] g1 = BitConverter.GetBytes(gpsLat);
            byte[] g2 = BitConverter.GetBytes(gpsLon);
            byte[] g3 = BitConverter.GetBytes(gpsAcc);
            byte[] c1 = BitConverter.GetBytes(cellLat);
            byte[] c2 = BitConverter.GetBytes(cellLon);
            byte[] c3 = BitConverter.GetBytes(cellAcc);
            byte[] n = enc.GetBytes(Settings.Name.ToCharArray());
            byte[] p = enc.GetBytes(Settings.PhoneNumber.ToCharArray());
            
            int dataLen = t.Length + g1.Length + g2.Length + g3.Length + c1.Length + c2.Length + c3.Length + n.Length + p.Length;

            byte[] d = BitConverter.GetBytes(dataLen);

            byte[] r = new byte[dataLen + m.Length + d.Length];

            
            // a to ponizej, poniewaz klasa MemoryStream nie dziala
            int o = 0;

            for (int i = 0; i < m.Length; i++)
            {
                r[o++] = m[i];
            }
            for (int i = 0; i < d.Length; i++)
            {
                r[o++] = d[i];
            }
            for (int i = 0; i < t.Length; i++)
            {
                r[o++] = t[i];
            }
            for (int i = 0; i < g1.Length; i++)
            {
                r[o++] = g1[i];
            }
            for (int i = 0; i < g2.Length; i++)
            {
                r[o++] = g2[i];
            }
            for (int i = 0; i < g3.Length; i++)
            {
                r[o++] = g3[i];
            }
            for (int i = 0; i < c1.Length; i++)
            {
                r[o++] = c1[i];
            }
            for (int i = 0; i < c2.Length; i++)
            {
                r[o++] = c2[i];
            }
            for (int i = 0; i < c3.Length; i++)
            {
                r[o++] = c3[i];
            }
            for (int i = 0; i < n.Length; i++)
            {
                r[o++] = n[i];
            }
            for (int i = 0; i < p.Length; i++)
            {
                r[o++] = p[i];
            }
            return r;
        }
    }
}
