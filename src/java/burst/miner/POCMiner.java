package burst.miner;

import java.io.File;
import akka.actor.ActorSystem;
import akka.actor.Props;
import burst.miner.*;
import java.util.*;
import nxt.Nxt;
import nxt.util.Logger;

public class POCMiner {
    public static MiningState miningState = new MiningState();
    public static ActorSystem miningActors = null;
    public static ActorSystem generateActors = null;
    
	public static void startGenerate(long addr, long startnonce, long plots, long staggeramt, int threads) {
		POCMiner.generateActors = ActorSystem.create();
		POCMiner.generateActors.actorOf(Props.create(GenerateSupr.class, new GenerateSupr.GenParams(addr, startnonce, plots, staggeramt, threads)));
	}
	
	public static void startPoolMining(String addr, int port) {
        if(POCMiner.miningState.running) {
            POCMiner.stopMining();
            POCMiner.miningState.reset();
        }
        POCMiner.miningState.pooled = true;
        POCMiner.miningState.addr = addr;
        POCMiner.miningState.port = port;
        
		POCMiner.miningActors = ActorSystem.create();
		POCMiner.miningActors.actorOf(Props.create(MinerPoolSupr.class, addr));
	}
    
    public static void startSoloMining(List<String> passphrases) {
        if(POCMiner.miningState.running) {
            POCMiner.stopMining();
            POCMiner.miningState.reset();
        }
        POCMiner.miningState.pooled = false;
        POCMiner.miningState.addr = "127.0.0.1";
        POCMiner.miningState.port = Nxt.getIntProperty("nxt.apiServerPort");
        
        POCMiner.miningActors = ActorSystem.create();
		POCMiner.miningActors.actorOf(Props.create(MinerSupr.class, passphrases));
    }
    
    public static String getUrl() {
        return POCMiner.miningState.addr+":"+POCMiner.miningState.port;
    }
    
    public static void stopMining() {
        if(POCMiner.miningActors != null) {
            POCMiner.miningActors.shutdown();
        }
    }
    
    public static void stopGenerate() {
        if(POCMiner.generateActors != null) {
            POCMiner.generateActors.shutdown();
        }
    }
    
    public static MiningState getMiningState() {
        if(POCMiner.miningState == null) {
        	POCMiner.miningState = new MiningState();
        }
        return POCMiner.miningState;
    }
    
    public static class MiningState {
        public String addr;
        public String plotDirPath;
        public int port;
        public boolean pooled;
        public boolean running;
        public List<Miner.PlotInfo> plots;
        public String bestResultAccountId;
        public String bestResultNonce;
        public String bestResultDeadline;
		public MiningState() {
			reset();
		}
        public void reset() {
            this.addr = "127.0.0.1";
			this.port = Nxt.getIntProperty("nxt.apiServerPort");
            this.pooled = false;
            this.running = false;
            this.plotDirPath = Nxt.getStringProperty("nxt.miningPlotDir");
            this.bestResultAccountId = "";
            this.bestResultNonce = "";
            this.bestResultDeadline = "";

            refreshPlotList();
        }
        
        public void refreshPlotList() {
        	this.plots = new ArrayList<Miner.PlotInfo>();
        	File plotDir = new File(this.plotDirPath);
            if(plotDir.exists() && plotDir.isDirectory()){
                File[] files = plotDir.listFiles();
                
                for(int i = 0; i < files.length; i++) {
                    Logger.logMessage("plot file "+files[i].getName());
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
    }
}
