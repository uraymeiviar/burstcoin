package nxt.http;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import burst.miner.POCMiner;
import nxt.http.APIServlet.APIRequestHandler;

public final class StopMining extends APIRequestHandler {
	static final StopMining instance = new StopMining();
	public StopMining() {
		super(new APITag[] {APITag.MINING, APITag.INFO});
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) {
		JSONObject response = new JSONObject();
		
		boolean result = POCMiner.stopMining();
			
		response.put("result", result);
		
		return response;
	}
}
