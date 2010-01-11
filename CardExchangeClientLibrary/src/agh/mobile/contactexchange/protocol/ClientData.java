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
	 * Client locations according to GPS, cellular location or cell ID/LAC
	 */
	public EnumMap<ClientLocationType, ClientLocation> locations =
		new EnumMap<ClientLocationType, ClientLocation>(ClientLocationType.class);
	
	/**
	 * Client contact info like name or phone number
	 */
	public Payload payload = new Payload();
}
