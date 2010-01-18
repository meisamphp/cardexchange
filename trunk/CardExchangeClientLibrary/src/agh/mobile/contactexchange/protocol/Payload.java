package agh.mobile.contactexchange.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.Serializable;
import java.io.IOException;

public class Payload implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String name;
	public String phoneNumber;
	
	//TODO: extend

	

	public void fromByteArray(byte[] data) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);
	
		name = dis.readUTF();
		phoneNumber = dis.readUTF();
	}
	
	public byte[] toByteArray() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		
		dos.writeUTF(name);
		dos.writeUTF(phoneNumber);

		return baos.toByteArray();
	}
}
