using System;
using System.Linq;
using System.Collections.Generic;
using System.Text;
using Rebex.Net;

namespace Mobilki.CardExchangeUtils
{
    class NTPClient
    {
        public static double getNtpTime()
        {
            // initialize the Ntp object 
            Ntp ntp = new Ntp(Constants.NTP_SERVER_URL);

            // get server time  
            NtpResponse response = ntp.GetTime();

            return response.TimeOffset.TotalSeconds;
        }
    }
}
