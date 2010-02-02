using System;
using System.Linq;
using System.Collections.Generic;
using System.Text;

namespace Mobilki
{
    class Constants
    {
        public const int BUFF_SIZE = 4096;

        public const string SERVER_IP = "192.168.1.101";
        public const int SERVER_PORT = 4444;

        public const string NTP_SERVER_URL = "tempus1.gum.gov.pl";
    }

    public enum State
    {
        IDLE,
        CONNECTED,
        DATA_SENT,
        PAIRLIST_RECEIVED,
        PAIRCHOICE_SENT,
        PAYLOAD_RECEIVED
    };

    public class MsgType
    {
        public const int CLIENT_DATA = 0;
        public const int PAIRLIST = 1;
        public const int PAIR_ID = 2;
        public const int PAYLOAD = 3;
        public const int TIMEOUT = 4;
        public const int EXCHANGE_DENIED = 5;
    }
}
