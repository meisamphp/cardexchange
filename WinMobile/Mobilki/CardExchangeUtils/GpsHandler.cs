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
        GpsPosition position = null;

        Gps gps = new Gps();

        public GpsHandler(MainScreen scr)
        {
            this.s = scr;
        }

        public void Load()
        {
            updateDataHandler = new EventHandler(UpdateData);
            gps.LocationChanged += new LocationChangedEventHandler(gps_LocationChanged);
        }

        protected void gps_LocationChanged(object sender, LocationChangedEventArgs args)
        {
            position = args.Position;

            // call the UpdateData method via the updateDataHandler so that we
            // update the UI on the UI thread
            s.Invoke(updateDataHandler);
        }

        void UpdateData(object sender, System.EventArgs args)
        {
            if (gps.Opened)
            {
                if (position != null)
                {
                    if (position.LatitudeValid)
                        Settings.gpsLat = position.Latitude;

                    if (position.LongitudeValid)
                        Settings.gpsLon = position.Longitude;
                    
                    /*if (position.TimeValid)
                    
                        str += "Time:\n   " + position.Time.ToString() + "\n";
                    */
                }
            }
        }

        public void Close()
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
