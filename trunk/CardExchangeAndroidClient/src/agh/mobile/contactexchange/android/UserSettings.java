package agh.mobile.contactexchange.android;

import java.io.Serializable;

import agh.mobile.contactexchange.protocol.Payload;
import android.content.SharedPreferences;

class UserSettings implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String name;
	private String phone;
	
	UserSettings(SharedPreferences prefs) {
		name = prefs.getString("name", "");
		phone = prefs.getString("phone", "");
	}
	
	public UserSettings(Payload payload) {
		name = payload.name;
		phone = payload.phoneNumber;
	}

	public boolean complete() {
		return name.length() > 0 && phone.length() > 0;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getPhone() {
		return phone;
	}

}
