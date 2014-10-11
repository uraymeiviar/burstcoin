package burst.miner;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.client.util.StringContentProvider;

import akka.actor.UntypedActor;
import akka.actor.Cancellable;
import scala.concurrent.duration.*;
import burst.miner.MinerSupr;
import nxt.util.Convert;
import nxt.Nxt;
import pocminer.POCMiner;

public class MinerPoolCom extends UntypedActor {

	String results;
	
	Cancellable tick = null;
	HttpClient client = null;
	SslContextFactory sslctx = null;
    int miningInfoTimeout;
    String miningAPIPath;
	
	MinerPoolSupr.NetPoolState laststate = null;
	
	public MinerPoolCom() {
		super();
		this.results = "";
        this.miningInfoTimeout = Nxt.getIntProperty("nxt.miningAPITimeout");
        this.miningAPIPath = Nxt.getStringProperty("nxt.miningAPIPath");
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof msgRefreshNetState) {
            
			// get new state
			String netstatetext = null;
			try {
				ContentResponse response = client.newRequest(POCMiner.getUrl() + this.miningAPIPath + "/getMiningInfo").timeout(this.miningInfoTimeout, TimeUnit.SECONDS).send();
				netstatetext = response.getContentAsString();
			}
			catch(Exception e) {
				System.out.println("Error: Unable to get mining info from wallet");
				return;
			}
			JSONObject netstatejson = (JSONObject)JSONValue.parse(netstatetext);
			if(!netstatejson.containsKey("height") || !netstatejson.containsKey("generationSignature") || !netstatejson.containsKey("baseTarget") ) {
				System.out.println("Error: Invalid json received");
				return;
			}
			String gsig = (String)netstatejson.get("generationSignature");
			if(gsig.length() != 64) {
				System.out.println("Invalid gensig received");
				return;
			}
			String h = (String)netstatejson.get("height");
			String bT = (String)netstatejson.get("baseTarget");
            
            String tD = Integer.toString(Integer.MAX_VALUE);
            if(netstatejson.containsKey("targetDeadline")) {
                tD = (String)netstatejson.get("targetDeadline");
            }
			
			byte[] b = Convert.parseHexString(gsig);
			long height = Convert.parseUnsignedLong(h);
			long baseTarget = Long.valueOf(bT);
			long targetDeadline = Long.valueOf(tD);
			MinerPoolSupr.NetPoolState state = new MinerPoolSupr.NetPoolState(height, b, baseTarget, targetDeadline);
			if(laststate == null || state.height != laststate.height || !(Arrays.equals(state.gensig, laststate.gensig))) {
				laststate = state;
				getContext().parent().tell(state, getSelf());
				System.out.println(netstatetext);
			}
		}
		else if(message instanceof MinerPoolSupr.msgAddResult) {
			MinerPoolSupr.msgAddResult newresult = (MinerPoolSupr.msgAddResult)message;
            POCMiner.miningState.bestResultAccountId = Long.toString(newresult.address);
            POCMiner.miningState.bestResultNonce = Long.toString(newresult.nonce);
            POCMiner.miningState.bestResultDeadline = newresult.deadline.toString();
			results += Convert.toUnsignedLong(newresult.address) + ":" + Convert.toUnsignedLong(newresult.nonce) + ":" + newresult.height + "\n";
		}
        else if(message instanceof MinerPoolSupr.msgSubmitResult) {
			try {
                POCMiner.miningState.bestResultAccountId = Long.toString(((MinerSupr.msgSubmitResult)message).accountId);
                POCMiner.miningState.bestResultNonce = Long.toString(((MinerSupr.msgSubmitResult)message).nonce);
                POCMiner.miningState.bestResultDeadline = ((MinerSupr.msgSubmitResult)message).result.toString();
				ContentResponse response = null;
                if(this.miningAPIPath.toLowerCase() != "pool") {
                    System.out.println("Submitting shares to pool");
                    
                    response = client.POST(POCMiner.getUrl() + this.miningAPIPath)
                    .param("requestType", "submitNonce")
                    .param("accountId", Long.toString(((MinerSupr.msgSubmitResult)message).accountId))
                    .param("nonce", Convert.toUnsignedLong(((MinerSupr.msgSubmitResult)message).nonce))
                    .timeout(this.miningInfoTimeout, TimeUnit.SECONDS)
                    .send();
                }
                
                if(response != null) {
                    String submitResult = response.getContentAsString();
                    System.out.println(submitResult);
                }
			}
			catch(Exception e) {
				System.out.println("Error: Failed to submit nonce");
			}
		}
		else if(message instanceof MinerPoolSupr.msgFlush) {
			System.out.println("Submitting shares to pool (flush)");
			try {
                ContentResponse response = null;
                if(this.miningAPIPath.toLowerCase() == "pool") {
                    if(results.equals("")) {
                        System.out.println("No valid shares to submit to pool");
                        return;
                    }
                    response = client.POST(POCMiner.getUrl() + this.miningAPIPath + "/submitWork")
                            .content(new StringContentProvider(results))
                            .timeout(this.miningInfoTimeout, TimeUnit.SECONDS)
                            .send();
                }
                else {
                    response = client.POST(POCMiner.getUrl() + this.miningAPIPath)
                        .param("requestType", "submitNonce")
                        .param("accountId", POCMiner.miningState.bestResultAccountId)
                        .param("nonce", POCMiner.miningState.bestResultNonce)
                        .timeout(this.miningInfoTimeout, TimeUnit.SECONDS)
                        .send();
                }
                
                if(response != null) {
                    String submitResult = response.getContentAsString();
                    System.out.println(submitResult);
                }
			}
			catch(Exception e) {
				System.out.println("Error: Failed to submit work to pool");
			}
			results = "";
		}
		else {
			unhandled(message);
		}
	}
	
	@Override
	public void preStart() {
		sslctx = new SslContextFactory(true);
		client = new HttpClient(sslctx);
		try {
			client.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		tick = getContext().system().scheduler().schedule(Duration.Zero(),
				Duration.create(10, TimeUnit.SECONDS),
				getSelf(),
				(Object)new msgRefreshNetState(),
				getContext().system().dispatcher(),
				null);
	}
	
	@Override
	public void postStop() {
		if(tick != null) {
			tick.cancel();
			tick = null;
		}
		if(client != null) {
			client = null;
		}
	}
	
	public static class msgRefreshNetState {}
}
