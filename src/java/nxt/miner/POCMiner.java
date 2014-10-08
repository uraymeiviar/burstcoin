package pocminer;

import java.io.IOException;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import pocminer.GenerateSupr;
import pocminer.MinerSupr;
import pocminer.Miner;
import nxt.Nxt;

public class POCMiner {
    public static MiningState miningState = new MiningState();
    public static ActorSystem miningActors = null;
    public static ActorSystem generateActors = null;
    
	public static void startGenerate(long addr, long startnonce, long plots, long staggeramt, int threads) {
		POCMiner.generateActors = ActorSystem.create();
		ActorRef gensupr = POCMiner.generateActors.actorOf(Props.create(GenerateSupr.class, new GenerateSupr.GenParams(addr, startnonce, plots, staggeramt, threads)));
	}
	
	public static void startPoolMining(String addr, int port) {
        if(this.miningState.running) {
            POCMiner.stopMining();
        }
        POCMiner.miningState.pooled = true;
        POCMiner.miningState.addr = addr;
        POCMiner.miningState.port = port;
        
		POCMiner.miningActors = ActorSystem.create();
		ActorRef minesupr = POCMiner.miningActors.actorOf(Props.create(MinerPoolSupr.class, addr));
	}
    
    public static void startSoloMining(List<String> passphrases) {
        if(POCMiner.miningState.running) {
            POCMiner.stopMining();
        }
        POCMiner.miningState.pooled = false;
        POCMiner.miningState.addr = "127.0.0.1";
        POCMiner.miningState.port = Nxt.getIntegerProperty("nxt.apiServerPort");
        
        POCMiner.miningActors = ActorSystem.create();
		ActorRef minesupr = POCMiner.miningActors.actorOf(Props.create(MinerSupr.class, passphrases));
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
        public reset() {
            this.addr = "127.0.0.1";
			this.port = Nxt.getIntegerProperty("nxt.apiServerPort");
            this.pooled = false;
            this.running = false;
            this.plotDirPath = Nxt.getStringProperty("nxt.miningPlotDir");
            
            File[] files = new File(plotDirPath).listFiles();
            for(int i = 0; i < files.length; i++) {
                Miner.PlotInfo pi = new Miner.PlotInfo(files[i].getName());
                this.plots.add(pi);
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
