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
        private Socket socket = null;
        private State state = State.IDLE;

        public MainScreen()
        {
            /* initialize this component */
            InitializeComponent();
        }


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

                case State.PAYLOAD_RECEIVED :
                    exchangeMenuItem.Enabled = true;
                    stateLabel.Text = "Contact saved.";
                    break;

                case State.IDLE :
                    exchangeMenuItem.Enabled = true;
                    stateLabel.Text = "Idle.";
                    break;
            }
        }

        
        private void exchangeButtonClick(object sender, EventArgs e)
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

                Settings.setLocation(cellIdLocation);
                Settings.setTime();

                sendData();
                if (receiveData() < 0)
                    return;

            }

        }

        
        private void sendData()
        {
            System.Net.IPAddress ipAdd = System.Net.IPAddress.Parse(Constants.SERVER_IP);
            System.Net.IPEndPoint remoteEP = new IPEndPoint(ipAdd, Constants.SERVER_PORT);
            socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
            socket.Connect(remoteEP);
            
            socket.Send(Settings.ToByteArray());

            setState(State.DATA_SENT);
        }


        private int receiveData()
        {
            Byte[] buff = new Byte[Constants.BUFF_SIZE];
            int read = socket.Receive(buff);

            if (read < 0)
            {
                disconnect("An error occured.");
                setState(State.IDLE);
                return -1;
            }
            
            if (read > ByteUtils.INT_SIZE)
            {
                while (read > 0)
                {
                    int pos = 0;

                    int type = ByteUtils.extractInt(buff, pos); // message type
                    pos += ByteUtils.INT_SIZE;

                    int len = ByteUtils.extractInt(buff, pos); // message length
                    pos += ByteUtils.INT_SIZE;

                    byte[] val = new byte[len];     // message data
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
                }
            }

            return 1;
        }

        
        private void handleData(int type, byte[] val)
        {
            switch (type)
            {
                case MsgType.PAIRLIST :
                    PairList partners = new PairList();
                    partners.fromByteArray(val);
                    setState(State.PAIRLIST_RECEIVED);
                    choosePair(partners);
                    break;

                case MsgType.PAYLOAD :
                    Payload payload = new Payload();
                    payload.fromByteArray(val);
                    addContact(payload);
                    setState(State.PAYLOAD_RECEIVED);
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


        private void addContact(Payload payload)
        {
            OutlookSession session = new OutlookSession();
            Contact contact = new Contact();
            contact.LastName = payload.name;
            contact.MobileTelephoneNumber = payload.phoneNumber;
            session.Contacts.Items.Add(contact);
            session.Dispose();
        }

        public void sendChoice(int id)
        {
            byte[] buff = new byte[3 * ByteUtils.INT_SIZE];

            int offset = ByteUtils.writeIntBytes(MsgType.PAIR_ID, buff, 0);
            offset = ByteUtils.writeIntBytes(4, buff, offset);
            offset = ByteUtils.writeIntBytes(id, buff, offset);
            
            socket.Send(buff);
            setState(State.PAIRCHOICE_SENT);

            if (receiveData() < 0)
            {
                disconnect("Error while sending choice.");
            }
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
            if (pairs.Count == 0)
            {
                exchangeMenuItem.Enabled = true;
                disconnect("No partners available, sorry ziom.");
                System.Diagnostics.Debug.WriteLine("No pairs, no pairs!");
            }
            else
            {
                PartnersScreen f = new PartnersScreen(pairs, this);
                Hide();
                f.Show();
            }
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