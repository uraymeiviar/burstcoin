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
        
        if(miningState.results != null) {
        	List<JSONObject> resultList = new ArrayList<JSONObject>();
        	for(Miner.msgBestResult result : miningState.results ) {
        		JSONObject resultJson = new JSONObject();
        		
        		resultJson.put("accountId",Long.toString(result.bestaddress));
        		resultJson.put("nonce",Long.toString(result.bestnonce));
        		resultJson.put("deadline",result.bestResult.toString());
        		
        		resultList.add(resultJson);
        	}
        	response.put("results", resultList);
        }
        
        if(miningState.plots != null) {
        	List<JSONObject> plotInfoList = new ArrayList<JSONObject>();
	        for(Miner.PlotInfo plotInfo : miningState.plots) {
	            JSONObject plot = new JSONObject();
	            
	            plot.put("filename",plotInfo.filename);
	            plot.put("accountId",Long.toString(plotInfo.address));
	            plot.put("startNonce",plotInfo.startnonce);
	            plot.put("nonceCount",plotInfo.plots);
	            plot.put("stagger",plotInfo.staggeramt);
	            plot.put("sizeMB",plotInfo.sizeMB);
	            
	            plotInfoList.add(plot);
	        }
	        
	        response.put("plots", plotInfoList);
        }
		
		return response;
	}
}
