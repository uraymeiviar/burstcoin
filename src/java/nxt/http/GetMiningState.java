package nxt.http;

import java.nio.ByteBuffer;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

import nxt.Block;
import nxt.Nxt;
import nxt.util.Convert;
import fr.cryptohash.Shabal256;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import pocminer.*;

public final class GetMiningState extends APIServlet.APIRequestHandler {
	static final GetMiningState instance = new GetMiningState();
	
	private GetMiningState() {
		super(new APITag[] {APITag.MINING, APITag.INFO});
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) {
		JSONObject response = new JSONObject();
        POCMiner.MiningState miningState = POCMiner.getMiningState();
		
		response.put("address", miningState.addr);
        response.put("port", miningState.port);
        response.put("plotDirPath", miningState.plotDirPath);
        response.put("pooled", miningState.pooled);
        response.put("running", miningState.running);
        response.put("bestResultAccountId", miningState.bestResultAccountId);
        response.put("bestResultNonce", miningState.bestResultNonce);
        response.put("bestResultDeadline", miningState.bestResultDeadline);
        
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
		
		return response;
	}
}
