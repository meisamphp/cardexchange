package agh.mobile.contactexchange.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

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

	public void fromByteArray(byte[] data) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);
		
		time = dis.readLong();
		gpsLatitude = dis.readDouble();
		gpsLongitude = dis.readDouble();
		gpsAccuracy = dis.readDouble();
		cellularLatitude = dis.readDouble();
		cellularLongitude = dis.readDouble();
		cellularAccuracy = dis.readDouble();
		cellId = dis.readLong();
		cellLac = dis.readLong();
		
		byte [] pl = new byte[dis.available()];
		dis.read(pl);
		payload.fromByteArray(pl);
	}

	
	public byte[] toByteArray() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);

		dos.writeLong(time);
		dos.writeDouble(gpsLatitude);
		dos.writeDouble(gpsLongitude);
		dos.writeDouble(gpsAccuracy);
		dos.writeDouble(cellularLatitude);
		dos.writeDouble(cellularLongitude);
		dos.writeDouble(cellularAccuracy);
		dos.writeLong(cellId);
		dos.writeLong(cellLac);
		dos.write(payload.toByteArray());

		return baos.toByteArray();
	}
}
