package nxt.http;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import burst.miner.POCMiner;
import nxt.http.APIServlet.APIRequestHandler;
import nxt.util.Convert;

public final class StartGenerate extends APIRequestHandler {
	static final StartGenerate instance = new StartGenerate();
	public StartGenerate() {
		super(new APITag[] {APITag.MINING, APITag.INFO});
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) {
		JSONObject response = new JSONObject();
		
		boolean result = false;
		
		String accountStr     = Convert.emptyToNull(req.getParameter("account"));
		String startNonceStr  = Convert.emptyToNull(req.getParameter("startNonce"));
		String nonceCountStr  = Convert.emptyToNull(req.getParameter("nonceCount"));
		String staggerSizeStr = Convert.emptyToNull(req.getParameter("staggerSize"));
		String threadCountStr = Convert.emptyToNull(req.getParameter("thread"));
		
		if(accountStr != null && startNonceStr != null && nonceCountStr != null) {
		
			if(staggerSizeStr == null) {
				staggerSizeStr = "256";
			}
			
			if(threadCountStr == null) {
				threadCountStr = "1";
			}
			
			long account = Long.parseLong(accountStr);
			long startNonce = Long.parseLong(startNonceStr);
			if(startNonce < 0){
				startNonce = 0;
			}
			long nonceCount = Long.parseLong(nonceCountStr);
			if(nonceCount < 1){
				nonceCount = 1;
			}
			long stagger = Long.parseLong(staggerSizeStr);
			if(stagger < 1) {
				stagger = 1;
			}
			int thread = Integer.parseInt(threadCountStr);
			if(thread < 1) {
				thread = 1;
			}
			
			result = POCMiner.startGenerate(account, startNonce, nonceCount, stagger, thread);
		}
			
		response.put("result", result);
		
		return response;
	}
}
