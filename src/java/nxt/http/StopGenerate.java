package nxt.http;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import burst.miner.POCMiner;
import nxt.http.APIServlet.APIRequestHandler;

public class StopGenerate extends APIRequestHandler {
	static final StopGenerate instance = new StopGenerate();
	public StopGenerate() {
		super(new APITag[] {APITag.MINING, APITag.INFO});
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) {
		JSONObject response = new JSONObject();
		
		boolean result = POCMiner.stopGenerate();
			
		response.put("result", result);
		
		return response;
	}
}
