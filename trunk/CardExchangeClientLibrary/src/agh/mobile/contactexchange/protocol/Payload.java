package agh.mobile.contactexchange.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.Serializable;
import java.io.IOException;


/**
 * Message containing data that clients are exchanging.
 * 
 * @author Witold Sowa <witold.sowa@gmail.com>
 */
public class Payload implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String name;
	public String phoneNumber;
	
	//TODO: extend with other fields like email, address, photo, etc.

	
	/**
	 * Deserialize a message
	 * 
	 * @param data Byte array of message body to deserialize from
	 * @throws IOException error occurred while reading bytes 
	 */
	public void fromByteArray(byte[] data) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);
	
		name = dis.readUTF();
		phoneNumber = dis.readUTF();
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
		
		dos.writeUTF(name);
		dos.writeUTF(phoneNumber);

		return baos.toByteArray();
	}
}
