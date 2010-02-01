using System;
using System.Linq;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using System.Net.Sockets;
using System.Net;
using Mobilki.CardExchangeUtils;
using Microsoft.WindowsMobile.PocketOutlook;


namespace Mobilki
{
    public partial class MainScreen : Form
    {
        private const int BUFF_SIZE = 4096;
        public int SelectedPairId = -1;

        private Socket socket = null;

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
            public const int PAIRLIST = 1;
            public const int PAIR_ID = 2;
            public const int PAYLOAD = 3;
            public const int TIMEOUT = 4;
            public const int EXCHANGE_DENIED = 5;
        }

        private State state = State.IDLE;

        private void setState(State s)
        {
            state = s;
            switch (s)
            {
                case State.CONNECTED :
                    exchangeMenuItem.Enabled = false;
                    stateLabel.Text = "Sending data...";
                    break;
                case State.DATA_SENT :
                    stateLabel.Text = "Waiting for server response...";
                    break;
                case State.PAIRCHOICE_SENT :
                    stateLabel.Text = "Choice sent...";
                    break;
                case State.PAIRLIST_RECEIVED :
                    stateLabel.Text = "Received pairlist.";
                    break;
                case State.IDLE :
                    exchangeMenuItem.Enabled = true;
                    stateLabel.Text = "Idle.";
                    break;
            }
            Refresh();
        }

        public MainScreen()
        {
            /* initialize this component */
            InitializeComponent();
        }

        private void button_Click(object sender, EventArgs e)
        {
            if (state != State.IDLE)
            {
                return;
            }
            
            if (Settings.Name.Length == 0 || Settings.PhoneNumber.Length == 0)
            {
                showSettingsScreen();
            }
            else
            {
                setState(State.CONNECTED);

                //GpsHandler h = new GpsHandler(this);
                //h.Load();
                Location cellIdLocation = new Location();
                try
                {
                    cellIdLocation = CellIdToLocation.extractLocation();
                }
                catch (Exception x)
                {
                    System.Diagnostics.Debug.WriteLine(x.StackTrace);
                }

                Settings.cellAcc = cellIdLocation.accuracy;
                Settings.cellLat = cellIdLocation.latitude;
                Settings.cellLon = cellIdLocation.longitude;
                Settings.time = DateTime.Now.Ticks;
                
                Refresh();

                sendData();
                if (receiveData() < 0)
                    return;

                disconnect("Idle.");
            }
        }

        private void sendData()
        {
            System.Net.IPAddress ipAdd = System.Net.IPAddress.Parse("192.168.1.101");
            System.Net.IPEndPoint remoteEP = new IPEndPoint(ipAdd, 4444);
            socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
            socket.Connect(remoteEP);
            byte[] buff = Settings.ToByteArray();
            socket.Send(buff);

            setState(State.DATA_SENT);
        }

        private int receiveData()
        {
            Byte[] buff = new Byte[BUFF_SIZE];
            int read = socket.Receive(buff);
            System.Diagnostics.Debug.WriteLine("Got msg");

            if (read < 0)
            {
                disconnect("An error occured.");
                setState(State.IDLE);
                return -1;
            }

            int intSize = 4;
            
            if (read > 4)
            {
                int pos = 0;
                while (read > 0)
                {
                    byte[] tmp = new byte[intSize];
                    Buffer.BlockCopy(buff, pos, tmp, 0, intSize);
                    Array.Reverse(tmp);
                    int type = BitConverter.ToInt32(tmp, 0);

                    pos += intSize;
                    
                    Buffer.BlockCopy(buff, pos, tmp, 0, intSize);
                    Array.Reverse(tmp);
                    int len = BitConverter.ToInt32(tmp, 0);
                    
                    pos += intSize;

                    byte[] val = new byte[len];
                    Buffer.BlockCopy(buff, pos, val, 0, len);
                                        
                    pos += len;

                    handleData(type, val);

                    if (read > pos)
                    {
                        byte[] rest = new byte[read - pos];
                        Buffer.BlockCopy(buff, pos, rest, 0, read - pos);
                        buff = rest;
                    }
                    read -= pos;
                    pos = 0;
                }
            }

            return 1;
        }

        private void handleData(int type, byte[] val)
        {
            switch (type)
            {
                case MsgType.PAIRLIST :
                    disconnect("pairlist!");
                    PairList partners = new PairList();
                    partners.fromByteArray(val);
                    setState(State.PAIRLIST_RECEIVED);
                    choosePair(partners);
                    sendChoice();
                    setState(State.PAIRCHOICE_SENT);
                    break;
                case MsgType.PAYLOAD :
                    disconnect("payload!");
                    Payload payload = new Payload();
                    payload.fromByteArray(val);
                    setState(State.PAYLOAD_RECEIVED);
                    OutlookSession session = new OutlookSession();
                    Contact contact = new Contact();
                    contact.LastName = payload.name;
                    contact.MobileTelephoneNumber = payload.phoneNumber;
                    session.Contacts.Items.Add(contact);
                    session.Dispose();
                    break;
                case MsgType.TIMEOUT :
                    disconnect("Disconnected due to timeout.");
                    break;
                case MsgType.EXCHANGE_DENIED :
                    disconnect("Card exchange denied.");
                    break;
                default:
                    System.Diagnostics.Debug.WriteLine("Wrong message type. Disconnecting.");
                    disconnect("Error occured");
                    break;
            }
        }

        private void sendChoice()
        {
            byte[] buff = new byte[2 * 4];
            Buffer.BlockCopy(BitConverter.GetBytes(MsgType.PAIR_ID), 0, buff, 0, 4);
            Buffer.BlockCopy(BitConverter.GetBytes(SelectedPairId), 0, buff, 4, 4);
            socket.Send(buff);
        }

        private void disconnect(String s)
        {
            if (socket.Connected)
            {
                socket.Close();
            }
            setState(State.IDLE);
            if (s.Length > 0)
            {
                stateLabel.Text = s;
            }
        }

        private void choosePair(PairList pairs)
        {
            PartnersScreen f = new PartnersScreen(pairs, this);
            Hide();
            f.Show();
        }

        private void showSettingsScreen()
        {
            SettingsScreen f = new SettingsScreen(this);
            Hide();     // hides current
            f.Show();     //shows new form
        }

        private void showSettingsScreen(object sender, EventArgs e)
        {
            showSettingsScreen();
        }

        private void exitMenuItemClicked(object sender, EventArgs e)
        {
            exit();
        }

        private void exit()
        {
            disconnect("");
            Close();
        }
    }
}