package agh.mobile.contactexchange.protocol;

import java.io.Serializable;

public class ClientLocation implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public double latitude; // in degrees
	public double longitude; // in degrees
	public double accuracy; // in meters

	public long cellId;
}
