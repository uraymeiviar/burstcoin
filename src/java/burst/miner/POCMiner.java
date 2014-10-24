package burst.miner;

import java.io.File;
import akka.actor.ActorSystem;
import akka.actor.Props;
import burst.miner.*;
import java.util.Date;
import java.util.*;
import nxt.Nxt;
import nxt.util.Logger;

public class POCMiner {
    public static MiningState miningState = new MiningState();
    public static GenerateState generateState = new GenerateState();
    public static ActorSystem miningActors = null;
    public static ActorSystem generateActors = null;
    private static Date lastGetstateTimestamp = new Date();
    
	public static boolean startGenerate(long addr, long startnonce, long plots, long staggeramt, int threads) {
		if(staggeramt > 8191) {
			staggeramt = 8192;
		}
		if(staggeramt < 256) {
			staggeramt = 256;
		}
		if(POCMiner.generateState.running) {
			if( POCMiner.generateState.account == addr && 
				POCMiner.generateState.startNonce == startnonce && 
				POCMiner.generateState.nonceCount == plots &&
				POCMiner.generateState.staggerSize == staggeramt ) {
				return true;
			}
            POCMiner.stopGenerate();
            POCMiner.generateState.reset();
        }
		try {
			POCMiner.generateActors = ActorSystem.create();
			POCMiner.generateActors.actorOf(Props.create(GenerateSupr.class, new GenerateSupr.GenParams(addr, startnonce, plots, staggeramt, threads)));
			POCMiner.generateState.running = true;
		}
        catch(Exception e){
        	POCMiner.generateState.running = false;
        }
        
        return POCMiner.generateState.running;
	}
	
	public static boolean startPoolMining(String addr, int port) {
        if(POCMiner.miningState.running) {
            POCMiner.stopMining();
            POCMiner.miningState.reset();
        }
        POCMiner.miningState.pooled = true;
        POCMiner.miningState.addr = addr;
        POCMiner.miningState.port = port;
        
        try {
        	POCMiner.miningActors = ActorSystem.create();
    		POCMiner.miningActors.actorOf(Props.create(MinerPoolSupr.class, addr));
    		POCMiner.miningState.running = true;
        }
        catch(Exception e){
        	POCMiner.miningState.running = false;
        }
        
        return POCMiner.miningState.running;
	}
    
    public static boolean startSoloMining(List<String> passphrases) {
        if(POCMiner.miningState.running) {
            POCMiner.stopMining();
            POCMiner.miningState.reset();
        }
        POCMiner.miningState.pooled = false;
        POCMiner.miningState.addr = "127.0.0.1";
        POCMiner.miningState.port = Nxt.getIntProperty("nxt.apiServerPort");
        
        try {
        	POCMiner.miningActors = ActorSystem.create();
    		POCMiner.miningActors.actorOf(Props.create(MinerSupr.class, passphrases));
    		POCMiner.miningState.running = true;
        }
        catch(Exception e) {
        	POCMiner.miningState.running = false;
        }
        
        return POCMiner.miningState.running;
    }
    
    public static String getUrl() {
        return POCMiner.miningState.addr+":"+POCMiner.miningState.port;
    }
    
    public static boolean stopMining() {
        if(POCMiner.miningActors != null) {
            POCMiner.miningActors.shutdown();
        }
        POCMiner.miningState.running = false;
        return true;
    }
    
    public static boolean stopGenerate() {
        if(POCMiner.generateActors != null) {
            POCMiner.generateActors.shutdown();
        }
        POCMiner.generateState.running = false;
        return true;
    }
    
    public static MiningState getMiningState() {
        if(POCMiner.miningState == null) {
        	POCMiner.miningState = new MiningState();
        }
        Date newTimestamp = new Date();
        long delta = newTimestamp.getTime() - POCMiner.lastGetstateTimestamp.getTime();
        if(delta > 5000) {
        	POCMiner.miningState.refreshPlotList();
        	POCMiner.lastGetstateTimestamp = newTimestamp;
        }
        return POCMiner.miningState;
    }
    
    public static GenerateState getGeneratingState() {
        if(POCMiner.generateState == null) {
        	POCMiner.generateState = new GenerateState();
        }

        return POCMiner.generateState;
    }
    
    public static class MiningState {
        public String addr;
        public String plotDirPath;
        public int port;
        public boolean pooled;
        public boolean running;
        public List<Miner.PlotInfo> plots;
        public List<Miner.msgBestResult> results;
		public MiningState() {
			reset();
		}
        public void reset() {
            this.addr = "127.0.0.1";
			this.port = Nxt.getIntProperty("nxt.apiServerPort");
            this.pooled = false;
            this.running = false;
            this.plotDirPath = Nxt.getStringProperty("nxt.miningPlotDir");
            this.results = new ArrayList<Miner.msgBestResult>();

            refreshPlotList();
        }
        
        public void refreshPlotList() {
        	this.plots = new ArrayList<Miner.PlotInfo>();
        	File plotDir = new File(this.plotDirPath);
            if(plotDir.exists() && plotDir.isDirectory()){
                File[] files = plotDir.listFiles();
                
                for(int i = 0; i < files.length; i++) {
                    Miner.PlotInfo pi = new Miner.PlotInfo(files[i]);
                    this.plots.add(pi);
                }
            }
            else {
                Logger.logMessage("Plot Dir path = "+plotDirPath+" not exist");
            }
        }
	}
    
    public static class GenerateState {
        public long startNonce;
        public long nonceCount;
        public long currentNonce;
        public long staggerSize;
        public int threadCount;
        public long account;
        public boolean running;
        public GenerateState() {
			reset();
		}
        
        public void reset() {
            this.startNonce = 0;
			this.nonceCount = 0;
            this.currentNonce = 0;
            this.staggerSize = 0;
            this.threadCount = 1;
            this.account = 0;
            this.running = false;
        }
    }
}
