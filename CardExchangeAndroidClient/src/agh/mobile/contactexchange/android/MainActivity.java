package agh.mobile.contactexchange.android;

import java.util.HashMap;
import java.util.List;

import agh.mobile.contactexchange.android.AndroidClient.Reason;
import agh.mobile.contactexchange.android.AndroidClient.State;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;
import android.provider.Contacts;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener, OnCancelListener, Callback, LocationListener {
	
	public static final int GET_SETTINGS_REQ = 1;
	public static final int GET_PARTNER_REQ = 2;
	
	public static final int GETTING_LOCATION_TIMEOUT = 30000;
	
	UserSettings settings;
	
	boolean hasGPS, hasCellular;
	Location gpsLocation;
	Location cellularLocation;	
	
	boolean settingsCanceled = false;
	
	SharedPreferences prefs;
	ProgressDialog progressDialog;
	Handler clientHandler, delayHandler;
	AndroidClient client;
	Thread clientThread;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        findViewById(R.id.SettingsBtn).setOnClickListener(this);
        findViewById(R.id.ExchangeBtn).setOnClickListener(this);
        findViewById(R.id.ExchangeBtn).setEnabled(false);
        
		clientHandler = new Handler(this);
		delayHandler = new Handler(this);     
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	
    	start();
    }

    @Override
    protected void onStop() {
    	super.onStop();
    	
    	stopGettingLocation();
    }

    private void start() {
	    prefs =  getSharedPreferences("userSetts", MODE_PRIVATE);
	
	    settings = new UserSettings(prefs);
	    if (settings.complete())
	    	settingsComplete();
	    else {
	    	setStatusText("Set your settings first.");
	    	if(!settingsCanceled)
	    		showSettings();
	    }
    }
    
	private void showSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		intent.putExtra("settings", settings);
		startActivityForResult(intent, GET_SETTINGS_REQ);
	}
	
	private void settingsComplete() {
        if (gpsLocation == null && cellularLocation == null)
        	startGettingLocation();
        else
    		enableExchanging();
	}    
	
	private void enableExchanging() {
		setStatusText("Ready.");
        findViewById(R.id.ExchangeBtn).setEnabled(true);
	}
	
	private void startExchange() {	
		long time = System.currentTimeMillis();
        findViewById(R.id.ExchangeBtn).setEnabled(false);
        
		progressDialog = ProgressDialog.show(this, "Exchanging card", "", true, true, this);
		progressDialog.setMessage("Connecting...");

		TelephonyManager tm  = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		int cellId = -1, cellLac = -1;
		if (tm != null) {
			CellLocation location = tm.getCellLocation();
			if (location instanceof GsmCellLocation)
				cellId = ((GsmCellLocation)location).getCid();
				cellLac = ((GsmCellLocation)location).getLac();
		}
        
		client = new AndroidClient(clientHandler);
		client.setExchangeTime(time);
		client.setSettings(settings);		
		client.setLocation(gpsLocation, cellularLocation, cellId, cellLac);
		
    	stopGettingLocation();
    	
		clientThread = new Thread(client);
		clientThread.start();
	}
	
	private void stopClient() {
		if (client != null)
			client.disconnect();
	}
	
	private void finishExchange() {
		clientThread = null;
		client = null;
		progressDialog.cancel();
	}
	
	private void setStatusText(String status) {
		((TextView)findViewById(R.id.StateText)).setText(status);
	}
	
	private void showErrorAlert(String msg) {
		new AlertDialog.Builder(this)
		.setMessage(msg)
		.setPositiveButton("OK", null)
		.setCancelable(false)
		.show();
	}
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case GET_SETTINGS_REQ:
			if (resultCode == RESULT_OK) {
				settings = (UserSettings) data.getSerializableExtra("response");
				SharedPreferences.Editor prefsEd = prefs.edit();
				prefsEd.putString("name", settings.getName());
				prefsEd.putString("phone", settings.getPhone());
				prefsEd.commit();
				settingsComplete();
				settingsCanceled = false;
			}
			else
				settingsCanceled = true;
			break;
		case GET_PARTNER_REQ:
			if (resultCode == RESULT_OK && client != null) {
				progressDialog.setMessage("Sending selected name...");
				client.selectPair(data.getIntExtra("partnerId", -1));
			}
			else {
				stopClient();
			}
			break;
		default:
			Log.e("onActivityResult", "Unrecognized request code");
			break;
		}
	}
    

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.SettingsBtn:
			showSettings();
			break;
		case R.id.ExchangeBtn:
			startExchange();
			break;
		default:
			Log.e("MainActivity", "Not recognized view clicked: id="+v.getId());
			break;
		}
	}

	public void onCancel(DialogInterface dialog) {
		Log.i("oncancel", "canceled progress dialog");	
		stopClient();
		start();
	}

	private void showPartnerSelectionList(HashMap<Integer, String> partners) {
		Log.i("selectPartner()", "got " + partners.size() + " partners");
		
		if (partners.isEmpty()) {
			stopClient();
			showErrorAlert("There is no one to exchange card with.");
		}
		else {
			Intent intent = new Intent(this, PartnersList.class);
			intent.putExtra("partners", partners);
			startActivityForResult(intent, GET_PARTNER_REQ);
		}
	}		
	
	private void saveCard(UserSettings card) {
		Log.i("saveCard", card.getName());

		stopClient();

    	Intent intent = new Intent();
    	intent.setAction(Contacts.Intents.SHOW_OR_CREATE_CONTACT);
    	intent.setData(Uri.fromParts("tel", card.getPhone() , null));
    	intent.putExtra(Contacts.Intents.EXTRA_CREATE_DESCRIPTION, card.getName());
    	intent.putExtra(Contacts.Intents.Insert.NAME, card.getName());
    	intent.putExtra(Contacts.Intents.Insert.PHONE_TYPE, Contacts.PhonesColumns.TYPE_MOBILE);
    	startActivity(intent);
	}
	
	private void handleStoppedState(Reason reason) {
		switch(reason) {
		case UNKNOWN_HOST:
			progressDialog.cancel();
			showErrorAlert("Cannot connect: Unknown host.");
			break;
		case IO_ERROR:
			progressDialog.cancel();
			showErrorAlert("Connection error.");
			break;
		case TIMEDOUT:
			showErrorAlert("Timedout.");
			stopClient();
			break;
		case DENIED:
			showErrorAlert("Selected partner didn't accept the exchange.");
			stopClient();
			break;
		}

	}

	
	@SuppressWarnings("unchecked")
	private void handleClientMessage(Message msg) {
		Log.i("handleClientMessage()", "State: " + msg.arg1 + ", Reason: " + msg.arg2);
		State state = State.values()[msg.arg1];
		Reason reason = Reason.values()[msg.arg2];
		
		switch(state) {
		case STOPPED:
			handleStoppedState(reason);
			break;
		case CONNECTED:
			progressDialog.setMessage("Sending card...");
			client.sendData();
			break;
		case DATA_SENT:
			progressDialog.setMessage("Waiting for response...");
			break;
		case GOT_PARTNERS:
			progressDialog.setMessage("Received exchange proposition");
			HashMap<Integer, String> partners = (HashMap<Integer, String>) msg.getData().getSerializable("partners");
			showPartnerSelectionList(partners);
			break;
		case PAIR_SENT:
			progressDialog.setMessage("Receiving card...");
			break;
		case GOT_CARD:
			progressDialog.setMessage("Disconnecting...");
			UserSettings card = (UserSettings) msg.getData().getSerializable("card");
			saveCard(card);
			break;
		case FINISHED:
			finishExchange();
		}
			
	}


	public boolean handleMessage(Message msg) {
		if (msg.getTarget() == clientHandler)
			handleClientMessage(msg);
		else if (msg.getTarget() == delayHandler) {
			if (!haveLocation())
				handleNoLocation();
		}
		else
			Log.e("handleMessage", "unknown message hanledr: " + msg.getTarget().toString());
		
		return false;
	}

	
	
	
	
	/* Location related methods */

	private void startGettingLocation() {
		setStatusText("Getting your location.");
		LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		List<String> providers = locMan.getAllProviders();
		if(providers.contains(LocationManager.GPS_PROVIDER) && locMan.isProviderEnabled(LocationManager.GPS_PROVIDER))
			hasGPS = true;
		if(providers.contains(LocationManager.NETWORK_PROVIDER) && locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			hasCellular = true;
		
		if (hasGPS) {
			locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
		}
		
		if (hasCellular) {
			locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, this);
		}
		
		if (!hasGPS && !hasCellular)
			handleNoLocation();
		else {
			Log.i("startGettingLocation(", "starting timeout");
			delayHandler.sendEmptyMessageDelayed(0, GETTING_LOCATION_TIMEOUT);
		}
	}
	
	private void stopGettingLocation() {
		LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locMan.removeUpdates(this);
		
		delayHandler.removeMessages(0);
	}
	
	private void handleNoLocation() {
		stopGettingLocation();
		setStatusText("Error while obtaining position");
		showErrorAlert("Cannot get your position");
	}
	
	private boolean haveLocation() {

		if (hasGPS && gpsLocation != null)
			return true;
		
		if (hasCellular && cellularLocation != null)
			return true;
		
		return false;
	}
	
	private void tryEnableExchanging() {
		
		if (!hasGPS && !hasCellular)
			handleNoLocation();
		else if(haveLocation())
			enableExchanging();
	}
	
	public void onLocationChanged(Location location) {
		Log.i("onLocationChanged()", location.getProvider() + ": lon: " + location.getLongitude() + ", lat: " + location.getLatitude());
		
		if(location.getProvider().equals(LocationManager.GPS_PROVIDER))
				gpsLocation = location;
		else if(location.getProvider().equals(LocationManager.NETWORK_PROVIDER))
			cellularLocation = location;

		tryEnableExchanging();
	}


	public void onProviderDisabled(String provider) {
		Log.i("onProviderDisabled()", provider);
	}

	public void onProviderEnabled(String provider) {
		Log.i("onProviderEnabled()", provider);
	}
	
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.i("onStatusChanged()", provider + ": status: " + status);
	}
}