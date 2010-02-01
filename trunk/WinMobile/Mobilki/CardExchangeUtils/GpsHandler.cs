using System;
using System.Linq;
using System.Collections.Generic;
using System.Text;
using Microsoft.WindowsMobile.Samples.Location;

namespace Mobilki
{
    class GpsHandler
    {

        private MainScreen s = null;
        private EventHandler updateDataHandler;
        GpsDeviceState device = null;
        GpsPosition position = null;

        Gps gps = new Gps();

        public GpsHandler(MainScreen scr)
        {
            this.s = scr;
        }

        public void Load()
        {
            updateDataHandler = new EventHandler(UpdateData);

            gps.DeviceStateChanged += new DeviceStateChangedEventHandler(gps_DeviceStateChanged);
            gps.LocationChanged += new LocationChangedEventHandler(gps_LocationChanged);
        }

        protected void gps_LocationChanged(object sender, LocationChangedEventArgs args)
        {
            position = args.Position;

            // call the UpdateData method via the updateDataHandler so that we
            // update the UI on the UI thread
            s.Invoke(updateDataHandler);
        }

        void gps_DeviceStateChanged(object sender, DeviceStateChangedEventArgs args)
        {
            device = args.DeviceState;

            // call the UpdateData method via the updateDataHandler so that we
            // update the UI on the UI thread
            s.Invoke(updateDataHandler);
        }

        void UpdateData(object sender, System.EventArgs args)
        {
            if (gps.Opened)
            {
                string str = "";
                if (device != null)
                {
                    str = device.FriendlyName + " " + device.ServiceState + ", " + device.DeviceState + "\n";
                }

                if (position != null)
                {

                    if (position.LatitudeValid)
                    {
                        str += "Latitude (DD):\n   " + position.Latitude + "\n";
                        str += "Latitude (D,M,S):\n   " + position.LatitudeInDegreesMinutesSeconds + "\n";
                    }

                    if (position.LongitudeValid)
                    {
                        str += "Longitude (DD):\n   " + position.Longitude + "\n";
                        str += "Longitude (D,M,S):\n   " + position.LongitudeInDegreesMinutesSeconds + "\n";
                    }

                    if (position.SatellitesInSolutionValid &&
                        position.SatellitesInViewValid &&
                        position.SatelliteCountValid)
                    {
                        str += "Satellite Count:\n   " + position.GetSatellitesInSolution().Length + "/" +
                            position.GetSatellitesInView().Length + " (" +
                            position.SatelliteCount + ")\n";
                    }

                    if (position.TimeValid)
                    {
                        str += "Time:\n   " + position.Time.ToString() + "\n";
                    }
                }

                s.stateLabel.Text = str;
            }
        }

        private void Close()
        {
            if (gps.Opened)
            {
                gps.Close();
            }
        }
      
        private void Start()
        {
            if (!gps.Opened)
            {
                gps.Open();
            }
        }

    }
}
