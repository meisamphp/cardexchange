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



public class AndroidClient extends AbstractClient implements Runnable {
	
	enum State {
		STOPPED,
		CONNECTED,
		DATA_SENT,
		GOT_PARTNERS,
		PAIR_SENT,		
		GOT_CARD,		
		FINISHED
	}
	
	enum Reason {
		NO_REASON,
		UNKNOWN_HOST,
		IO_ERROR,
		TIMEDOUT,
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
		
		msg.arg1 = State.GOT_PARTNERS.ordinal();
		msg.arg2 = Reason.NO_REASON.ordinal();
		
		data.putSerializable("partners", pairs);
		msg.setData(data);
		handler.sendMessage(msg);
	}

	public void handlePayload(Payload payload) {
		Message msg = new Message();
		Bundle data = new Bundle();
		
		msg.arg1 = State.GOT_CARD.ordinal();
		msg.arg2 = Reason.NO_REASON.ordinal();
		
		UserSettings card = new UserSettings(payload);
		
		data.putSerializable("card", card);
		msg.setData(data);
		handler.sendMessage(msg);

	}
	public void handleTimedout() {
		disconnect();
		setState(State.STOPPED, Reason.TIMEDOUT);
	}

	private void setState(State state) {
		setState(state, Reason.NO_REASON);
	}
	
	private void setState(State state, Reason reason) {
		Message m = new Message();
		m.arg1 = state.ordinal();
		m.arg2 = reason.ordinal();
		handler.sendMessage(m);
	}
	
	private void connect() throws Exception {
		try {
			init();
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
	
	public void sendData() {
		cd.time = getNtpTime(exchangeTime);
		try {
			getConnection().sendClientData(cd);
			setState(State.DATA_SENT);
		} catch (IOException e) {
			setState(State.STOPPED, Reason.IO_ERROR);
			disconnect();
			e.printStackTrace();
		}
	}
	
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
		setState(State.STOPPED);
		try {
			connect();
		}
		catch (Exception e) {
			return;
		}
		
		while (isConnected)
			try {
				getConnection().readData();
			} catch (IOException e) {
				setState(State.STOPPED, Reason.IO_ERROR);
				disconnect();
				e.printStackTrace();
			}
		setState(State.FINISHED);
	}

	public void setLocation(Location gpsLocation, Location cellularLocation, int cid, int lac) {
		
		if(gpsLocation != null) {
			cd.gpsLatitude = gpsLocation.getLatitude();
			cd.gpsLongitude = gpsLocation.getLongitude();
			cd.gpsAccuracy = gpsLocation.getAccuracy();
		}
		else
			cd.gpsAccuracy = -1.0;
		
		if(cellularLocation != null) {
			cd.cellularLatitude = cellularLocation.getLatitude();
			cd.cellularLongitude = cellularLocation.getLongitude();
			cd.cellularAccuracy = cellularLocation.getAccuracy();
		}
		else
			cd.gpsAccuracy = -1.0;
		
		cd.cellId = cid;
		cd.cellLac = lac;
	}

	public void setSettings(UserSettings settings) {
		cd.payload.name = settings.getName();
		cd.payload.phoneNumber = settings.getPhone();		
	}
	
	public void setExchangeTime(long time) {
		exchangeTime = time;	
	}

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
