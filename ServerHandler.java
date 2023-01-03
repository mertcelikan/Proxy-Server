package deneme;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;



import java.io.*;
public class ServerHandler extends Thread {

	Socket clientSocket;
	DataInputStream dis;
	DataOutputStream dos;

	public ServerHandler(Socket clientSocket) throws Exception {
		this.clientSocket = clientSocket;
		dis = new DataInputStream(clientSocket.getInputStream());
		dos = new DataOutputStream(clientSocket.getOutputStream());
		
	}

	@Override
	public void run() {

		try {

			byte[] headerArr = new byte[5000];
			int hc = 0;

			// only for header part
			while (true) {
				byte i = (byte) dis.read();
				headerArr[hc++] = i;
				if (headerArr[hc - 1] == '\n' && headerArr[hc - 2] == '\r' && headerArr[hc - 3] == '\n'
						&& headerArr[hc - 4] == '\r') { // \r\n\r\n
					break;
				}

			}

			
			String header = new String(headerArr, 0, hc);
			System.out.println("-------HEADER FROM CLIENT----");
			System.out.println(header);

			// GET / HTTP/1.1\r\n
			// Host: asd.com
			//
			int fsp = header.indexOf(' ');
			int ssp = header.indexOf(' ', fsp + 1);
			int eol = header.indexOf("\r\n");

			String methodName = header.substring(0, fsp);

			String restHeader = header.substring(eol + 2);

			String modHeader = restHeader;

			if (modHeader.contains("Proxy-Connection")) {
				int proxIndex = modHeader.indexOf("Proxy-Connection");
				int eolProxIndex = modHeader.indexOf("\r\n", proxIndex);

				modHeader = modHeader.substring(0, proxIndex) + modHeader.substring(eolProxIndex + 2);
			}

			if (modHeader.contains("Cookie: Unwanted Cookie")) {
				int cookieIndex = modHeader.indexOf("Cookie: Unwanted Cookie");
				int eolCookieIndex = modHeader.indexOf("\r\n", cookieIndex);

				modHeader = modHeader.substring(0, cookieIndex) + modHeader.substring(eolCookieIndex + 2);
			}

			System.out.println("--MOD HEADER --");
			System.out.println(modHeader);

			String fullpath = header.substring(fsp + 1, ssp);

			URL url = new URL(fullpath);

			String domain = url.getHost();
			String shortpath = url.getPath().equals("") ? "/" : url.getPath();

			System.out.println(domain);
			System.out.println(shortpath);
			
			
			

			if (methodName.equals("GET")) {
				if (ProxyServer.forbiddenAddresses.contains(domain)) {
					
				
					String html =	"<html>\r\n" +
										"<head>\r\n" +
											"<title>401 Not Authorized</title>\r\n" +
										"</head>\r\n" +
										"<body>\r\n" +
											"<h1>401 Not Authorized</h1>\r\n" +
											"<h1>The domain " + domain + " is forbidden</h1>\r\n" +
										"</body>\r\n" +
									"</html>\r\n";

					String response =	"HTTP/1.1 401 Not Authorized\r\n" +
										"Server: CSE471Proxy\r\n" +
										"Content-Type: text/html\r\n" +
										"Content-Length: " + html.length() + "\r\n" +
										"\r\n" +
										html;

					dos.writeBytes(response);
					writeError("127.0.0.1",domain,shortpath,methodName,"401");

				} else if (ProxyServer.cache.containsKey(fullpath)) {
					handleProxy(methodName, modHeader, null, domain, shortpath, fullpath, true);
				} else {
					handleProxy(methodName, modHeader, null, domain, shortpath, fullpath, false);
				}

			} else if (methodName.equals("POST")) {

				int contIndex = header.indexOf("Content-Length: ");
				int eol2 = header.indexOf("\r\n", contIndex);
				String contSize = header.substring(contIndex + 16, eol2);
				int contSizeInt = Integer.parseInt(contSize);

				System.out.println("Header from client ContLength: " + contSizeInt);

				byte[] headerPayload = new byte[contSizeInt];

				byte[] buffer = new byte[1024];

				int sum = 0;
				int read;

				while (sum < contSizeInt) {
					read = dis.read(buffer);
					System.arraycopy(buffer, 0, headerPayload, sum, read);
					sum += read;
				}

				handleProxy(methodName, modHeader, headerPayload, domain, shortpath, null, false);

			} else {

				String html =	"<html>\r\n" +
									"<head>\r\n" +
										"<title>405 Method Not Allowed</title>\r\n" +
									"</head>\r\n" +
									"<body>\r\n" +
										"<h1>405 Method Not Allowed</h1>\r\n" +
										"<h1>The HTTP Method " + methodName + " is not allowed</h1>\r\n" +
									"</body>\r\n" +
								"</html>\r\n";

				String response =	"HTTP/1.1 405 Method Not Allowed\r\n" +
									"Server: CSE471Proxy\r\n" +
									"Content-Type: text/html\r\n" +
									"Content-Length: " + html.length() + "\r\n" +
									"\r\n" +
									html;

				dos.writeBytes(response);
				writeError("127.0.0.1",domain,shortpath,methodName,"405");

			}

			System.out.println("HANDLED CLIENT "
					+ clientSocket.getInetAddress().getHostAddress()
					+ " FOR ADDRESS " + fullpath);

		} catch (Exception e) {
			e.printStackTrace();
			
		}

	}

	private void handleProxy(String methodName, String restHeader, byte[] headerPayload, String domain,
			String shortpath, String url, boolean condGet) throws Exception {

		Socket proxiedSocket = new Socket(domain, 80);

		DataInputStream dis1 = new DataInputStream(proxiedSocket.getInputStream());
		DataOutputStream dos1 = new DataOutputStream(proxiedSocket.getOutputStream());

		String constructedHeader = null;

		// construct request
		if (methodName.equals("GET") && condGet) {
			String lM = ProxyServer.cache.get(url).lastModified;
			constructedHeader = methodName + ' ' + shortpath + " HTTP/1.1\r\n"
								+ "If-Modified-Since: " + lM + "\r\n"
								+ restHeader;
		} else {
			constructedHeader = methodName + ' ' + shortpath + " HTTP/1.1\r\n"
								+ restHeader;
		}

		System.out.println("-------HEADER TO WEBSERVER----");
		System.out.println(constructedHeader);

		dos1.writeBytes(constructedHeader);

		if (methodName.equals("POST") && headerPayload != null) {
			dos1.write(headerPayload);
		}

		// NOW READ HTTP RESPONSE FROM WEBSERVER

		// byte array for HTTP Response header
		byte[] reponseHdrArr = new byte[5000];
		int rc = 0;

		// only for response header part
		while (true) {
			byte i = (byte) dis1.read();
			reponseHdrArr[rc++] = i;
			if (reponseHdrArr[rc - 1] == '\n' && reponseHdrArr[rc - 2] == '\r' && reponseHdrArr[rc - 3] == '\n'
					&& reponseHdrArr[rc - 4] == '\r') { // \r\n\r\n
				break;
			}

		}

		System.out.println("-------RESPONSE HEADER FROM WEBSERVER----");
		String responseHdr = new String(reponseHdrArr, 0, rc);
		System.out.println(responseHdr);

		int fsp = responseHdr.indexOf(' ');
		int ssp = responseHdr.indexOf(' ', fsp + 1);
		int eol = responseHdr.indexOf("\r\n");

		String statusCode = responseHdr.substring(fsp + 1, ssp);

		if (statusCode.equals("200")) {
			// header part of response back to client
			dos.write(reponseHdrArr, 0, rc);

			String date = null;
			long contSizeInt = -1;

			int lMIndex = responseHdr.indexOf("Last-Modified: ");
			if (lMIndex >= 0) {
				int eolLM = responseHdr.indexOf("\r\n", lMIndex);
				date = responseHdr.substring(lMIndex + 15, eolLM);
				System.out.println("FOUND LAST-MODIFIED IN RESPONSE: " + date);
			}

			int contIndex = responseHdr.indexOf("Content-Length: ");
			if (contIndex >= 0) {
				int eolCont = responseHdr.indexOf("\r\n", contIndex);
				String contSize = responseHdr.substring(contIndex + 16, eolCont);
				contSizeInt = Long.parseLong(contSize);
				System.out.println("FOUND DATA SIZE IN RESPONSE: " + contSizeInt);
			}

			if (methodName.equals("GET") && date != null && contSizeInt <= 10000000) { // caching items max 10MB
				System.out.println("Caching payload since its less than 10MB");
				byte[] payload = new byte[(int) contSizeInt];

				byte[] buffer = new byte[1024];

				int sum = 0;
				int read;

				// payload part of the response back to client
				while (sum < contSizeInt) {
					read = dis1.read(buffer);
					System.arraycopy(buffer, 0, payload, sum, read);
					sum += read;
				}

				dos.write(payload);
				dos.flush();

				ProxyServer.cache.put(url, new CacheElement(date, (int) contSizeInt, payload));
			} else {
				System.out.println("Proxying a large file...");
				byte[] buffer = new byte[5120000]; // a large buffer for big downloads

				long sum = 0;
				int read;

				// payload part of the response back to client
				while (sum < contSizeInt) {
					read = dis1.read(buffer);
					dos.write(buffer, 0, read);
					sum += read;
				}

				System.out.println("Sent " + sum + " bytes to client...");
				
				dos.flush();
			}
		} else if (statusCode.equals("304")) {
			System.out.println("-------RESPONSE IS 304 NOT MODIFIED----");
			CacheElement c = ProxyServer.cache.get(url);
			String newResponseHdr = "HTTP/1.1 200 OK\r\n"
					+ "Content-Length: " + c.length + "\r\n"
					+ responseHdr.substring(eol + 2);
			System.out.println("-------RESPONSE HEADER TO CLIENT----");
			System.out.println(newResponseHdr);
			dos.writeBytes(newResponseHdr);

			dos.write(c.data, 0, c.length);
			dos.flush();
			writeError("127.0.0.1",domain,shortpath,methodName,"304");
		}

		System.out.println();
		System.out.println("SENT HTTP RESPONSE & DATA BACK TO CLIENT");
		System.out.println();

		proxiedSocket.close();

	}

	public void writeError(String clientAddr,String domain,String path, String method,String status) {
		String strTotal = null;
		Date d = new Date();
		 TimeZone gmt = TimeZone.getTimeZone("GMT");
		 SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm:ss zzz");
		 sdf.setTimeZone(gmt);
		 String sdf_date = sdf.format(d);
		 strTotal = strTotal+sdf_date+"|"+clientAddr+"|"+domain+"|"+path+"|"+method+"|"+status+"|"+"\n";
		 ProxyServer.strTotal.add(strTotal);
	}
}