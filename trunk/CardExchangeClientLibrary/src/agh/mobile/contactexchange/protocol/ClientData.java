package agh.mobile.contactexchange.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Client data message. This message contains client NTP time,
 * its location data and a payload data a client would like to exchange.
 * 
 * @author Witold Sowa <witold.sowa@gmail.com>
 */
public class ClientData implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * NTP time of pushing "Exchange"
	 */
	public long time; 
	
	/**
	 * Client latitude according to GPS
	 */
	public double gpsLatitude;
	
	/**
	 * Client longitude according to GPS
	 */
	public double gpsLongitude;
	
	/**
	 * GPS position accuracy in meters or -1.0 if GPS position is unknown
	 * or 0.0 if position is known but accuracy is unknown.
	 */
	public double gpsAccuracy;  

	/**
	 * Client latitude according to cellular network estimation
	 */
	public double cellularLatitude;
	
	/**
	 * Client longitude according to cellular network estimation
	 */
	public double cellularLongitude;
	
	/**
	 * Cellular position accuracy in meters or -1.0 if position is unknown
	 * or 0.0 if position is known but accuracy is unknown.
	 */
	public double cellularAccuracy;
	
	/**
	 * Client contact info like name or phone number
	 */
	public Payload payload = new Payload();

	
	
	/**
	 * Deserialize a message
	 * 
	 * @param data Byte array of message body to deserialize from
	 * @throws IOException error occurred while reading bytes 
	 */
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
		
		byte [] pl = new byte[dis.available()];
		dis.read(pl);
		payload.fromByteArray(pl);
	}

	
	/**
	 * Serialize a message
	 * 
	 * @return Byte array with serialized message body
	 * @throws IOException error occurred while writing bytes 
	 */
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
		dos.write(payload.toByteArray());

		return baos.toByteArray();
	}
}
