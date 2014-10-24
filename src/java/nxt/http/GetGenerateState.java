package nxt.http;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import burst.miner.POCMiner;
import nxt.http.APIServlet.APIRequestHandler;

public final class GetGenerateState extends APIRequestHandler {
	static final GetGenerateState instance = new GetGenerateState();
	
	private GetGenerateState() {
		super(new APITag[] {APITag.MINING, APITag.INFO});
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) {
        
        POCMiner.GenerateState generateState = POCMiner.getGeneratingState();
		JSONObject response = new JSONObject();
		
		response.put("startNonce", generateState.startNonce);
        response.put("nonceCount", generateState.nonceCount);
        response.put("currentNonce", generateState.currentNonce);
        response.put("staggerSize", generateState.staggerSize);
        response.put("threadCount", generateState.threadCount);
        response.put("account", generateState.account);
        response.put("running", generateState.running);
		
		return response;
	}
}
