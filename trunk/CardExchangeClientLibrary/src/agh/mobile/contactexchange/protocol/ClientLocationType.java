package agh.mobile.contactexchange.protocol;

public enum ClientLocationType {
	/**
	 * GPS based location (latitude/longitude)
	 */
	GPS,
	
	/**
	 * cellular network based location (latitude/longitude)
	 */
	NETWORK,
	
	/**
	 * cellular network based location (cellId/cellLac)
	 */
	CELL_ID
}
