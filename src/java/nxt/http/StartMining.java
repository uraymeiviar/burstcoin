package nxt.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import burst.miner.*;
import nxt.http.APIServlet.APIRequestHandler;
import nxt.util.Convert;

public final class StartMining extends APIServlet.APIRequestHandler {
	static final StartMining instance = new StartMining();
	
	private StartMining() {
		super(new APITag[] {APITag.MINING, APITag.INFO});
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) {
		JSONObject response = new JSONObject();
		
		String passphraseList = Convert.emptyToNull(req.getParameter("passphrases"));
		if(passphraseList == null || passphraseList == "") {
			String host = Convert.emptyToNull(req.getParameter("host"));
			String port = Convert.emptyToNull(req.getParameter("port"));
			
			if(host == null || host == ""){
				host = "127.0.0.1";
			}
			
			if(port == null || port == ""){
				port = "8125";
			}
			
			boolean result = POCMiner.startPoolMining(host, Integer.parseInt(port));
			
			response.put("result", result);
		}
		else {
			List<String> items = new ArrayList<String>(Arrays.asList(passphraseList.split("\\s*,\\s*")));
			boolean result = POCMiner.startSoloMining(items);
			
			response.put("result", result);
		}
		
		return response;
	}
}
