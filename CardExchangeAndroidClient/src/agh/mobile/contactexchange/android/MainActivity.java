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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * Main application activity. Show main screen and allows user to start
 * exchange. Takes care of location updates and starts thread running
 * exchange client.
 * 
 * @author wsowa
 *
 */
public class MainActivity extends Activity implements OnClickListener, OnCancelListener, Callback, LocationListener {
	
	public static final int GET_SETTINGS_REQ = 1;
	public static final int GET_PARTNER_REQ = 2;
	
	public static final int GETTING_LOCATION_TIMEOUT = 30000; // 30 sec.
	
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
    	// start client
    	start();
    }

    @Override
    protected void onStop() {
    	super.onStop();
    	// stop getting location updates when stopping client.
    	stopGettingLocation();
    }

    
    /**
     * Start the client,
     */
    private void start() {
	    prefs =  getSharedPreferences("userSetts", MODE_PRIVATE);
	
	    // check whether user setting are complete and show settings activity
	    // if not.
	    settings = new UserSettings(prefs);
	    if (settings.complete())
	    	settingsComplete();
	    else {
	    	setStatusText("Set your settings first.");
	    	if(!settingsCanceled)
	    		showSettings();
	    }
    }
    
	/**
	 * Shows settings activity
	 */
	private void showSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		intent.putExtra("settings", settings);
		startActivityForResult(intent, GET_SETTINGS_REQ);
	}
	
	/**
	 * Settings are complete. Check location info
	 */
	private void settingsComplete() {
		// check weather client have location info.
        if (gpsLocation == null && cellularLocation == null)
        	// start getting location if it doesn't
        	startGettingLocation();
        else
        	// or enable exchanging if it does
    		enableExchanging();
	}    
	
	/**
	 * Enables exchanging
	 */
	private void enableExchanging() {
		setStatusText("Ready.");
        findViewById(R.id.ExchangeBtn).setEnabled(true);
	}
	
	/**
	 * Start actual exchange. This method is called whan the 
	 * "Exchange" button is pushed.
	 */
	private void startExchange() {	
		// get the time of pushing the button
		long time = System.currentTimeMillis();
		
		// disable exchanging button
        findViewById(R.id.ExchangeBtn).setEnabled(false);
        
        // show exchange progress dialog
		progressDialog = ProgressDialog.show(this, "Exchanging card", "", true, true, this);
		progressDialog.setMessage("Connecting...");
        
		// create client implementation passing communication handler
		client = new AndroidClient(clientHandler);
		
		// set time of pushing the button
		client.setExchangeTime(time);
		// set settings
		client.setSettings(settings);
		// set location info
		client.setLocation(gpsLocation, cellularLocation);
		
		// stop getting location updates
    	stopGettingLocation();
    	
		// create and start the actual exchange client thread.
    	clientThread = new Thread(client);
		clientThread.start();
	}
	
	/**
	 * Stop the exchange client
	 */
	private void stopClient() {
		if (client != null)
			client.disconnect();
	}
	
	/**
	 * Finalize the exchange.
	 */
	private void finishExchange() {
		// remove the client thread and cancel progress dialog
		clientThread = null;
		client = null;
		progressDialog.cancel();
	}
	
	/**
	 * Set status text
	 * 
	 * @param status test to set in status label
	 */
	private void setStatusText(String status) {
		((TextView)findViewById(R.id.StateText)).setText(status);
	}
	
	/**
	 * Show error alert.
	 * 
	 * @param msg Alert text message
	 */
	private void showErrorAlert(String msg) {
		new AlertDialog.Builder(this)
		.setMessage(msg)
		.setPositiveButton("OK", null)
		.setCancelable(false)
		.show();
	}
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		
		// settings activity has finished
		case GET_SETTINGS_REQ:
			if (resultCode == RESULT_OK) {
				// store new settings in shared properties
				settings = (UserSettings) data.getSerializableExtra("response");
				SharedPreferences.Editor prefsEd = prefs.edit();
				prefsEd.putString("name", settings.getName());
				prefsEd.putString("phone", settings.getPhone());
				prefsEd.commit();
				
				// try to enable exchange (check location before)
				settingsComplete();
				settingsCanceled = false;
			}
			else
				settingsCanceled = true;
			break;
			
		// partners list activity finished
		case GET_PARTNER_REQ:
			if (resultCode == RESULT_OK && client != null) {
				// send id of selected partner
				progressDialog.setMessage("Sending selected name...");
				client.selectPair(data.getIntExtra("partnerId", -1));
			}
			else {
				// no partner selected - stop client
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
		// exchange canceled. restart client.
		stopClient();
		start();
	}

	/**
	 * Start partners list activity.
	 * 
	 * @param partners. List of potential partners to exchange with.
	 */
	private void showPartnerSelectionList(HashMap<Integer, String> partners) {
		Log.i("selectPartner()", "got " + partners.size() + " partners");
		
		// don't start activity if partners list is empty. show alert instead
		if (partners.isEmpty()) {
			stopClient();
			showErrorAlert("There is no one to exchange card with.");
		}
		// start activity
		else {
			Intent intent = new Intent(this, PartnersList.class);
			intent.putExtra("partners", partners);
			startActivityForResult(intent, GET_PARTNER_REQ);
		}
	}		
	
	/**
	 * Save another client vCard
	 * 
	 * @param card Contact info of another client
	 */
	private void saveCard(UserSettings card) {
		Log.i("saveCard", card.getName());
		
		// message exchange with server is finished now. stop the client.
		stopClient();

		// start contacts create contact activity
    	Intent intent = new Intent();
    	intent.setAction(Contacts.Intents.SHOW_OR_CREATE_CONTACT);
    	intent.setData(Uri.fromParts("tel", card.getPhone() , null));
    	intent.putExtra(Contacts.Intents.EXTRA_CREATE_DESCRIPTION, card.getName());
    	intent.putExtra(Contacts.Intents.Insert.NAME, card.getName());
    	intent.putExtra(Contacts.Intents.Insert.PHONE_TYPE, Contacts.PhonesColumns.TYPE_MOBILE);
    	startActivity(intent);
	}
	
	/**
	 * Handle client state change to stopped state
	 * 
	 * @param reason the reason of state change.
	 */
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

	
	/**
	 * Handle messages coming from exchange client. Message contains
	 * state change info: new state and change reason
	 * 
	 * @param msg Message from exchange client.
	 */
	@SuppressWarnings("unchecked")
	private void handleClientMessage(Message msg) {
		Log.i("handleClientMessage()", "State: " + msg.arg1 + ", Reason: " + msg.arg2);
		State state = State.values()[msg.arg1];
		Reason reason = Reason.values()[msg.arg2];
		
		switch(state) {
		
		case STOPPED:
			// client has stopped due to an error. check the reason.
			handleStoppedState(reason);
			break;
			
		case CONNECTED:
			// client has connected successfully. Send own data.
			progressDialog.setMessage("Sending card...");
			client.sendData();
			break;
			
		case DATA_SENT:
			// data sent. waiting for partners list.
			progressDialog.setMessage("Waiting for response...");
			break;
			
		case GOT_PARTNERS:
			// partners list received. store it and start partners list activity.
			progressDialog.setMessage("Received exchange proposition");
			HashMap<Integer, String> partners = (HashMap<Integer, String>) msg.getData().getSerializable("partners");
			showPartnerSelectionList(partners);
			break;
			
		case PAIR_SENT:
			// selected partner id sent. wait for partners card (or denied message)
			progressDialog.setMessage("Receiving card...");
			break;
			
		case GOT_CARD:
			// received partners card. save it in contacts.
			progressDialog.setMessage("Disconnecting...");
			UserSettings card = (UserSettings) msg.getData().getSerializable("card");
			saveCard(card);
			break;
			
		case FINISHED:
			// client reached last state. finalize exchange
			finishExchange();
		}
			
	}


	public boolean handleMessage(Message msg) {
		if (msg.getTarget() == clientHandler)
			// handle message from exchange client
			handleClientMessage(msg);
		
		else if (msg.getTarget() == delayHandler) {
			// handle message from location delay timer
			
			if (!haveLocation()) {
				// handle no location if couldn't obtain
				// any location data before timeout
				handleNoLocation();
			}
		}
		else
			Log.e("handleMessage", "unknown message hanledr: " + msg.getTarget().toString());
		
		return false;
	}

	
	
	/* Location related methods */

	
	/**
	 * Register for location updates.
	 */
	private void startGettingLocation() {
		setStatusText("Getting your location.");
		
		LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		List<String> providers = locMan.getAllProviders();
		if(providers.contains(LocationManager.GPS_PROVIDER) && locMan.isProviderEnabled(LocationManager.GPS_PROVIDER))
			hasGPS = true;
		if(providers.contains(LocationManager.NETWORK_PROVIDER) && locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			hasCellular = true;
		

		if (hasGPS) {
			// register for gps location updates
			locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
		}
		
		if (hasCellular) {
			// register for cellular location updates
			locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, this);
		}
		
		if (!hasGPS && !hasCellular)
			// no location providers available. show error.
			handleNoLocation();
		else {
			Log.i("startGettingLocation(", "starting timeout");
			
			// start timer for location timeout. 
			delayHandler.sendEmptyMessageDelayed(0, GETTING_LOCATION_TIMEOUT);
		}
	}
	
	/**
	 * Unregister from location updates
	 */
	private void stopGettingLocation() {
		LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		// unregister
		locMan.removeUpdates(this);
		
		// stop location timer
		delayHandler.removeMessages(0);
	}
	
	/**
	 * Handle no location data error.
	 */
	private void handleNoLocation() {
		// stop getting location
		stopGettingLocation();
		
		// show error message
		setStatusText("Error while obtaining position");
		showErrorAlert("Cannot get your position");
	}
	
	/**
	 * @return True if have any location data
	 */
	private boolean haveLocation() {

		if (hasGPS && gpsLocation != null)
			return true;
		
		if (hasCellular && cellularLocation != null)
			return true;
		
		return false;
	}
	
	/**
	 * Check whether have any location info and. Show error
	 * if not or enable exchanging otherwise. 
	 */
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