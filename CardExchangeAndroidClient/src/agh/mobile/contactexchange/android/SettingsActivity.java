package agh.mobile.contactexchange.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

/**
 * An Android activity allowing user to set data that he is willing to
 * exchange. There are fields "settings" and "response" in calling Intent
 * and in result which contain serialized UserSettings object.
 * 
 * @author wsowa
 *
 */
public class SettingsActivity extends Activity implements OnClickListener {
	
	UserSettings settings;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        
        settings = (UserSettings) getIntent().getExtras().getSerializable("settings");
        
        EditText et;
        et = (EditText) findViewById(R.id.NameEdit);
        et.setText(settings.getName());
        et = (EditText) findViewById(R.id.PhoneEdit);
        et.setText(settings.getPhone());
        
        findViewById(R.id.OkBtn).setOnClickListener(this);
    }

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.OkBtn:
			setSettings();
			break;
		default:
			Log.e("SettingsActivity", "Not recognized view clicked: id="+v.getId());
			break;
		}
	}
	
	private void setSettings() {
		EditText et;
		
		// get text from Name EditText and store it in UserSettings object
        et = (EditText) findViewById(R.id.NameEdit);
        settings.setName(et.getText().toString());
        
		// get text from Phone EditText and store it in UserSettings object
        et = (EditText) findViewById(R.id.PhoneEdit);
        settings.setPhone(et.getText().toString());

        // if settings are complete (i.e. are both set) then finish activity
        if (settings.complete()) {
        	Intent intent = getIntent();
        	intent.putExtra("response", settings);
        	setResult(RESULT_OK, intent);
        	finish();
        }
        // if setting are not complete then show error alert.
        else {
        	new AlertDialog.Builder(this)
        	.setMessage("You must fill all settings fields")
        	.setPositiveButton("OK", null)
        	.setCancelable(false)
        	.show();
        }
        	
	}
}