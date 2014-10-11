package nxt.http;

import java.util.*;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import burst.miner.*;

public final class GetMiningState extends APIServlet.APIRequestHandler {
	static final GetMiningState instance = new GetMiningState();
	
	private GetMiningState() {
		super(new APITag[] {APITag.MINING, APITag.INFO});
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) {
        
        POCMiner.MiningState miningState = POCMiner.getMiningState();
		JSONObject response = new JSONObject();
		
		response.put("address", miningState.addr);
        response.put("port", miningState.port);
        response.put("plotDirPath", miningState.plotDirPath);
        response.put("pooled", miningState.pooled);
        response.put("running", miningState.running);
        response.put("bestResultAccountId", miningState.bestResultAccountId);
        response.put("bestResultNonce", miningState.bestResultNonce);
        response.put("bestResultDeadline", miningState.bestResultDeadline);
        
        if(miningState.plots != null) {
        	List<JSONObject> plotInfoList = new ArrayList<JSONObject>();
	        for(Miner.PlotInfo plotInfo : miningState.plots) {
	            JSONObject plot = new JSONObject();
	            
	            plot.put("filename",plotInfo.filename);
	            plot.put("accountId",plotInfo.address);
	            plot.put("startNonce",plotInfo.startnonce);
	            plot.put("nonceCount",plotInfo.plots);
	            plot.put("stagger",plotInfo.staggeramt);
	            
	            plotInfoList.add(plot);
	        }
	        
	        response.put("plots", plotInfoList);
        }
		
		return response;
	}
}
