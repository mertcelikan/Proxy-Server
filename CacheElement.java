package deneme;

public class CacheElement {

	String lastModified;
	int length;
	byte[] data;

	public CacheElement(String lM, int len, byte[] d) {
		lastModified = lM;
		length = len;
		data = d;
	}

}
