package agh.mobile.contactexchange.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

public class PairsList extends HashMap<Integer, String> {

	public void fromByteArray(byte[] data) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);
	
		int n = dis.readInt();
		for (int i = 0; i < n; i++) {
			int id = dis.readInt();
			String name = dis.readUTF();
			put(id, name);
		}
	}
	
	public byte[] toByteArray() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		
		int n = size();
		dos.writeInt(n);
		
		for (Entry<Integer, String> e : entrySet()) {
			dos.writeInt(e.getKey());
			dos.writeUTF(e.getValue());
		}

		return baos.toByteArray();
	}
}
