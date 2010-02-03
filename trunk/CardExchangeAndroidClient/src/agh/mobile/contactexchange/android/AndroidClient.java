package agh.mobile.contactexchange.android;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import agh.mobile.contactexchange.client.AbstractClient;
import agh.mobile.contactexchange.protocol.ClientData;
import agh.mobile.contactexchange.protocol.Payload;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


/**
 * Android exchange client implementation. It runs as a separate
 * thread waiting for data from server.
 * 
 * @author wsowa
 *
 */
public class AndroidClient extends AbstractClient implements Runnable {
	
	/**
	 * Client states
	 * 
	 * @author wsowa
	 *
	 */
	enum State {
		
		/**
		 * Client is stopped
		 */
		STOPPED,
		
		/**
		 * Client is conneted to server 
		 */
		CONNECTED,
		
		/**
		 * Client sent own data 
		 */
		DATA_SENT,
		
		/**
		 * Client received partners list 
		 */
		GOT_PARTNERS,
		
		
		/**
		 * Client sent partner selection
		 */
		PAIR_SENT,
		
		/**
		 * Client receiver partners data
		 */
		GOT_CARD,
		
		/**
		 * Client finished to work 
		 */
		FINISHED
	}
	
	/**
	 * Reason of client state change
	 * 
	 * @author wsowa
	 *
	 */
	enum Reason {
		
		/**
		 * No reason of change. Change was typical client behavior. 
		 */
		NO_REASON,
		
		/**
		 * Client couldn't resolve host name
		 */
		UNKNOWN_HOST,
		
		/**
		 * Client encountered an error in data transmission.
		 */
		IO_ERROR,
		
		/**
		 * Client has timed out (received TIMEOUT message form the server). 
		 */
		TIMEDOUT,
		
		/**
		 * Client was denied to exchange (received "denied" message).
		 */
		DENIED
	}
	
	Handler handler;
	
	ClientData cd = new ClientData();
	
	long exchangeTime;

	boolean isConnected = false;
	
	public AndroidClient(Handler handler) {
		super();

		this.handler = handler;
	}

	public void handleDisconnected() {
		/* 
		 * Don't do anything. We send notification
		 *  when connection is closed from run() method 
		 */
	}

	public void handleExchangeDenied() {
		disconnect();
		setState(State.STOPPED, Reason.DENIED);
	}

	public void handlePartnersList(Map<Integer, String> partners) {
		Message msg = new Message();
		Bundle data = new Bundle();
		
		HashMap<Integer, String> pairs = new HashMap<Integer, String>(partners);
		
		// change state to GOT_PARTNERS
		msg.arg1 = State.GOT_PARTNERS.ordinal();
		msg.arg2 = Reason.NO_REASON.ordinal();
		
		// attach partners list to state change message
		data.putSerializable("partners", pairs);
		
		msg.setData(data);
		handler.sendMessage(msg);
	}

	public void handlePayload(Payload payload) {
		Message msg = new Message();
		Bundle data = new Bundle();
		
		// change state to GOT_CARD
		msg.arg1 = State.GOT_CARD.ordinal();
		msg.arg2 = Reason.NO_REASON.ordinal();
		
		UserSettings card = new UserSettings(payload);
		
		// attach partner's card to state change message
		data.putSerializable("card", card);
		
		msg.setData(data);
		handler.sendMessage(msg);

	}
	public void handleTimedout() {
		disconnect();
		setState(State.STOPPED, Reason.TIMEDOUT);
	}

	/**
	 * Set client state. Has the same effect like 
	 * {@link #setState(State, Reason)} with NO_REASON second argument
	 * 
	 * @param state a new state.
	 */
	private void setState(State state) {
		setState(state, Reason.NO_REASON);
	}
	
	/**
	 * Set client state.
	 * @param state a new state.
	 * @param reason a reason of state change.
	 */
	private void setState(State state, Reason reason) {
		Message m = new Message();
		m.arg1 = state.ordinal();
		m.arg2 = reason.ordinal();
		handler.sendMessage(m);
	}
	
	/**
	 * Connect to the exchange server.
	 * 
	 * @throws Exception if error occurred
	 */
	private void connect() throws Exception {
		try {
			// initialize client first (this gets NTP time offset for example)
			init();
			
			// connect to server.
			getConnection().connect(getServerAddress());
			isConnected = true;
			setState(State.CONNECTED);
		}
		catch (UnknownHostException e) {
			setState(State.STOPPED, Reason.UNKNOWN_HOST);
			throw e;
		}
		catch (IOException e) {
			setState(State.STOPPED, Reason.IO_ERROR);
			throw e;
		}
	}
	
	/**
	 * Send own data to the server.
	 */
	public void sendData() {
		// put NTP time of exchange to the message
		cd.time = getNtpTime(exchangeTime);
		try {
			//sent message
			getConnection().sendClientData(cd);
			setState(State.DATA_SENT);
		} catch (IOException e) {
			setState(State.STOPPED, Reason.IO_ERROR);
			disconnect();
			e.printStackTrace();
		}
	}
	
	/**
	 * Sent selected partner id
	 * @param id id of selected partner
	 */
	public void selectPair(int id) {
		 try {
			getConnection().sendPairId(id);
			setState(State.PAIR_SENT);
		} catch (IOException e) {
			setState(State.STOPPED, Reason.IO_ERROR);
			disconnect();
			e.printStackTrace();
		}
	}
	
	public void run() {
		//set initial client state
		setState(State.STOPPED);
		
		try {
			// connect to server
			connect();
		}
		catch (Exception e) {
			return;
		}
		
		// main client loop.
		while (isConnected)
			try {
				// try to read data from server.
				getConnection().readData();
			} catch (IOException e) {
				setState(State.STOPPED, Reason.IO_ERROR);
				disconnect();
				e.printStackTrace();
			}
			
		// when the loop is finished. put client to finished state.
		setState(State.FINISHED);
	}

	/**
	 * Set client location data
	 * 
	 * @param gpsLocation client location according to GPS.
	 * @param cellularLocation client location according to Cellular estimation.
	 */
	public void setLocation(Location gpsLocation, Location cellularLocation) {
		
		// set GPS location data
		if(gpsLocation != null) {
			cd.gpsLatitude = gpsLocation.getLatitude();
			cd.gpsLongitude = gpsLocation.getLongitude();
			cd.gpsAccuracy = gpsLocation.getAccuracy();
			Log.i("gpsLoc", "" + cd.gpsLatitude + " " + cd.gpsLongitude + " " + cd.gpsAccuracy);
		}
		else
			// or indicate that GPS position is not available
			cd.gpsAccuracy = -1.0;
		
		// set cellular location data
		if(cellularLocation != null) {
			cd.cellularLatitude = cellularLocation.getLatitude();
			cd.cellularLongitude = cellularLocation.getLongitude();
			cd.cellularAccuracy = cellularLocation.getAccuracy();
			Log.i("cellLoc", "" + cd.cellularLatitude + " " + cd.cellularLongitude + " " + cd.cellularAccuracy);
		}
		else
			// or indicate that cellular position is not available
			cd.cellularAccuracy = -1.0;
	}

	/**
	 * Set own vCard.
	 * 
	 * @param settings own contact information to exchange
	 */
	public void setSettings(UserSettings settings) {
		cd.payload.name = settings.getName();
		cd.payload.phoneNumber = settings.getPhone();		
	}
	
	/**
	 * Set time of pushing "exchange" button. Given time will be adjusted by
	 * NTP time offset
	 * 
	 * @param time local time of pushing the button.
	 */
	public void setExchangeTime(long time) {
		exchangeTime = time;	
	}

	/**
	 * Disconnect client from the server.
	 */
	public void disconnect() {
		if (!isConnected)
			return;
		
		try {
			getConnection().disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		isConnected = false;
	}

}
