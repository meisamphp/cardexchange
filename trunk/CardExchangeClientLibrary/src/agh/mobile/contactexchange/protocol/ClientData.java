package agh.mobile.contactexchange.protocol;

import java.io.Serializable;
import java.util.EnumMap;

public class ClientData implements Serializable {
	private static final long serialVersionUID = 1L;
	
	
	/**
	 * NTP time of pushing "Exchange"
	 */
	public long time; 
	
	/**
	 * Client location according to GPS
	 */
	public double gpsLatitude;
	public double gpsLongitude;
	/*
	 * position accuracy in meters or -1.0 if GPS position is unknown
	 * or 0.0 if position is known but accuracy is unknown.
	 */
	public double gpsAccuracy;  

	/**
	 * Client location according to cellular network estimation
	 */
	public double cellularLatitude;
	public double cellularLongitude;
	/*
	 * position accuracy in meters. Same rules as for gpsAccuracy field.
	 */
	public double cellularAccuracy;
	
	/**
	 * Client GSM cell ID and LAC. -1 if unknown
	 */
	public long cellId;
	public long cellLac;
	
	/**
	 * Client contact info like name or phone number
	 */
	public Payload payload = new Payload();
}
