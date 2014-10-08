package pocminer;

import static akka.actor.SupervisorStrategy.resume;

import java.io.File;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import nxt.util.Convert;
import fr.cryptohash.Shabal256;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.actor.Cancellable;
import akka.actor.SupervisorStrategy.Directive;
import akka.japi.Function;
import scala.concurrent.duration.Duration;
import pocminer.MinerSupr;
import pocminer.ScoopChecker.msgBestScoop;
import pocminer.ScoopReader;
import pocminer.ScoopChecker;
import pocminer.PoolScoopChecker;
import nxt.util.MiningPlot;
import nxt.Nxt;

public class MinerPool extends UntypedActor {
	MinerPoolSupr.NetPoolState state;
	
	ActorRef reader;
	ActorRef poolChecker;
	
	BigInteger bestresult;
	long bestaddr;
	long bestnonce;
	Boolean newbest;
	
	int scoopnum;
	
	Cancellable tick = null;
	
	public Miner(MinerPoolSupr.NetPoolState state) {
		super();
		this.state = state;
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof ScoopReader.msgScoopChunk) {
            poolChecker.tell(new PoolScoopChecker.msgCheckPoolScoops((ScoopReader.msgScoopChunk)message, state.gensig, state.baseTarget, state.targetDeadline, state.height), getSelf());
		}
		else if(message instanceof ScoopChecker.msgBestScoop) {
			ScoopChecker.msgBestScoop bs = (msgBestScoop) message;
			if(bs.result.compareTo(bestresult) < 0) {
				bestresult = bs.result;
				bestaddr = bs.address;
				bestnonce = bs.nonce;
				newbest = true;
			}
		}
		else if(message instanceof MinerSupr.msgAddResult) {
			getContext().parent().tell(message, getSelf());
		}
		else if(message instanceof msgSendResults) {
			if(newbest) {
				newbest = false;
				getContext().parent().tell(new msgBestResult(bestaddr, bestnonce, bestresult), getSelf());
			}
		}
		else if(message instanceof msgReadFlush) {
			poolChecker.tell(new msgCheckFlush(), getSelf());
		}
		else if(message instanceof msgCheckFlush) {
			getContext().parent().tell(new MinerSupr.msgFlush(), getSelf());
		}
		else {
			unhandled(message);
		}
		
	}
	
	@Override
	public void preStart() {
		init();
	}
	
	@Override
	public void postStop() {
		if(tick != null) {
			tick.cancel();
			tick = null;
		}
	}
	
	private void init() {
		bestresult = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);
		bestaddr = 0;
		bestnonce = 0;
		newbest = false;

		reader = getContext().actorOf(Props.create(ScoopReader.class));
		//checker = getContext().actorOf(Props.create(ScoopChecker.class));
		poolChecker = getContext().actorOf(Props.create(PoolScoopChecker.class));
		
		ByteBuffer buf = ByteBuffer.allocate(32 + 8);
		buf.put(state.gensig);
		buf.putLong(state.height);
		
		Shabal256 md = new Shabal256();
		md.update(buf.array());
		BigInteger hashnum = new BigInteger(1, md.digest());
		scoopnum = hashnum.mod(BigInteger.valueOf(MiningPlot.SCOOPS_PER_PLOT)).intValue();
        
        String plotDir = Nxt.getStringProperty("nxt.miningPlotDir");
		
		File[] files = new File(plotDir).listFiles();
		for(int i = 0; i < files.length; i++) {
			PlotInfo pi = new PlotInfo(files[i].getName());
            reader.tell(new ScoopReader.msgReadScoops(pi, scoopnum), getSelf());
		}
		reader.tell(new msgReadFlush(), getSelf());
		
		tick = getContext().system().scheduler().schedule(Duration.create(10, TimeUnit.SECONDS),
				Duration.create(5, TimeUnit.SECONDS),
				getSelf(),
				new msgSendResults(),
				getContext().system().dispatcher(),
				null);
	}
	
	public static class PlotInfo {
		public String filename;
		public long address;
		public long startnonce;
		public long plots;
		public long staggeramt;
		public PlotInfo(String filename) {
			this.filename = filename;
			String[] parts = filename.split("_");
			this.address = Convert.parseUnsignedLong(parts[0]);
			this.startnonce = Long.valueOf(parts[1]);
			this.plots = Long.valueOf(parts[2]);
			this.staggeramt = Long.valueOf(parts[3]);
		}
	}
	
	public static class msgBestResult {
		public long bestaddress;
		public long bestnonce;
        public BigInteger bestResult;
		public msgBestResult(long bestaddress, long bestnonce, BigInteger bestResult) {
			this.bestaddress = bestaddress;
			this.bestnonce = bestnonce;
            this.bestResult = bestResult;
		}
	}
	
	public static class msgSendResults {}
	
	public static class msgReadFlush {}
	public static class msgCheckFlush {}
	
	private static SupervisorStrategy strategy =
		new OneForOneStrategy(10, Duration.create("1 minute"),
			new Function<Throwable, Directive>() {
				@Override
				public Directive apply(Throwable t) {
					return resume();
				}
		});
	
	@Override
	public SupervisorStrategy supervisorStrategy() {
		return strategy;
	}
}