package local.rest.interfaces;

import com.sun.net.httpserver.*;
import net.sf.json.*;

public interface Is4Resource {
	public  void get(HttpExchange exchange, boolean internalCall, JSONObject internalResp);
	public void put(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp);
	public void post(HttpExchange exchange, String data, boolean internalCall, JSONObject internalResp);
	public void delete(HttpExchange exchange, boolean internalCall, JSONObject internalResp);
	public void sendResponse(HttpExchange exchange, int errorCode, String response, boolean internalCall, JSONObject internalResp);
}
