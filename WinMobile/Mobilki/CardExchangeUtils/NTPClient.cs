using System;
using System.Linq;
using System.Collections.Generic;
using System.Text;
using Rebex.Net;

namespace Mobilki.CardExchangeUtils
{
    class NTPClient
    {
        public static void getNtpTime()
        {
            // initialize the Ntp object 
            Ntp ntp = new Ntp("time.nist.gov");

            // get server time  
            NtpResponse response = ntp.GetTime();

            // get time difference  
            Console.WriteLine(response);
        }
    }
}
