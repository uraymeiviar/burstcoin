package pocminer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import scala.concurrent.duration.Duration;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import static akka.actor.SupervisorStrategy.resume;
import akka.actor.UntypedActor;
import akka.japi.Function;

public class MinerPoolSupr extends UntypedActor {

	NetPoolState state;
	
	ActorRef poolCom = null;
	ActorRef miner = null;
	
	public MinerSupr() {
		super();
		this.state = null;
		init();
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof NetPoolState) {
			state = (NetPoolState)message;
			
			if(miner != null) {
				getContext().stop(miner);
			}
			miner = getContext().actorOf(Props.create(MinerPool.class, state));
		}
		else if(message instanceof Miner.msgBestResult) {
			System.out.print("New best: ");
			System.out.print(Convert.toUnsignedLong(((Miner.msgBestResult)message).bestaddress));
			System.out.print(":");
			System.out.println(((Miner.msgBestResult)message).bestnonce);
			
			poolCom.tell(new msgSubmitResult(((Miner.msgBestResult)message).bestaddress,
                                         ((Miner.msgBestResult)message).bestnonce), getSelf());
		}
		else if(message instanceof MinerSupr.msgAddResult) {
			System.out.println("Found pool share: " + Convert.toUnsignedLong(((MinerSupr.msgAddResult)message).address) + ":" + Convert.toUnsignedLong(((MinerSupr.msgAddResult)message).nonce));
			poolCom.tell(message, getSelf());
		}
		else if(message instanceof msgFlush) {
			poolCom.tell(message, getSelf());
		}
		else {
			unhandled(message);
		}
		
	}
	
	private void init() {
		poolCom = getContext().actorOf(Props.create(MinerPoolCom.class));
	}
	
	public static class NetPoolState {
		public long height;
		public byte[] gensig;
		public long baseTarget;
		public long targetDeadline;
		public NetPoolState(long height, byte[] gensig, long baseTarget, long targetDeadline) {
			this.height = height;
			this.gensig = gensig;
			this.baseTarget = baseTarget;
			this.targetDeadline = targetDeadline;
		}
	}
	
	public static class msgSubmitResult {
		public String accountId;
		public long nonce;
		public msgSubmitResult(String accountId, long nonce) {
			this.accountId = accountId;
			this.nonce = nonce;
		}
	}
	
	public static class msgAddResult {
		public long address;
		public long nonce;
		public long height;
        public BigInteger deadline
		public msgAddResult(long address, long nonce, long height, BigInteger deadline) {
			this.address = address;
			this.nonce = nonce;
			this.height = height;
            this.deadline = deadline;
		}
	}
	
	public static class msgFlush {}
	
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
