package agh.mobile.contactexchange.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Pairs list  message. This message contains a list of clients that
 * a client can be paired with.
 * 
 * @author wsowa
 */
public class PairsList extends HashMap<Integer, String> {
	private static final long serialVersionUID = 1L;

	/**
	 * Deserialize a message
	 * 
	 * @param data Byte array of message body to deserialize from
	 * @throws IOException error occurred while reading bytes 
	 */
	public void fromByteArray(byte[] data) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);
	
		// read number of partners in message
		int n = dis.readInt();
		
		for (int i = 0; i < n; i++) {
			// read partner client id
			int id = dis.readInt();
			
			// read partner name
			String name = dis.readUTF();
			
			// put id and name to dictionary
			put(id, name);
		}
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
		
		// write the int32 with number of partners first.
		int n = size();
		dos.writeInt(n);
		
		// for each partner
		for (Entry<Integer, String> e : entrySet()) {
			// write int32 with partner client id 
			dos.writeInt(e.getKey());
			// string with partner name
			dos.writeUTF(e.getValue());
		}

		return baos.toByteArray();
	}
}
