package agh.mobile.contactexchange.protocol;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

public class ClientData implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public long time;
	public Map<ClientLocationType, ClientLocation> locations =
		new EnumMap<ClientLocationType, ClientLocation>(ClientLocationType.class);
	public Payload payload = new Payload();
}
