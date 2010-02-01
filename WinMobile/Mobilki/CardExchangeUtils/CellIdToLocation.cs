using System;
using System.Linq;
using System.Collections.Generic;
using System.Collections;
using System.Text;
using System.Net;
using System.IO;

namespace Mobilki
{
    class CellIdToLocation
    {
        public static Location extractLocation()
        {
            string cellInfoFull = RIL.GetCellTowerInfo();
            Location l = new Location();
            if (cellInfoFull.StartsWith("Fail"))
            {
                return l;
            }
            string[] cellInfo = cellInfoFull.Split(new char[] { '-' });
            Console.WriteLine(cellInfo[0]);
            Console.WriteLine(cellInfo[1]);
            cellInfo[0] = Convert.ToInt64(cellInfo[0]).ToString("X");
            cellInfo[1] = Convert.ToInt64(cellInfo[1]).ToString("X");
            WebRequest oRequest = WebRequest.Create(
                  String.Format("http://cellid.labs.ericsson.net/json/lookup?cellid={0}&mnc={2}&mcc={3}&lac={1}&key=Y8UeWww2RHY8FMwplM5KtIXakU7QMBw2D1njGxya", cellInfo));
            WebResponse oResponse = oRequest.GetResponse(); 
            StreamReader oReader = new StreamReader(oResponse.GetResponseStream());
            string sContent = oReader.ReadToEnd();
            Hashtable h = (Hashtable)JSON.JsonDecode(sContent);

            l.latitude = double.Parse(((Hashtable)h["position"])["latitude"].ToString());
            l.longitude = double.Parse(((Hashtable)h["position"])["longitude"].ToString());
            l.accuracy = double.Parse(((Hashtable)h["position"])["accuracy"].ToString());

            return l;
        }
    }

    class Location
    {
        public double latitude = 0;
        public double longitude = 0;
        public double accuracy = 0;
    }
}
