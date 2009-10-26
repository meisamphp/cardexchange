package agh.mobile.contactexchange.protocol;

import java.io.Serializable;

public class ClientData implements Serializable {

	public long time;
	public double latitude;
	public double longitude;
	public int cellId;
	public Payload payload;
}
