package anz.service;

import anz.SampleUser;
import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RestController
public class FxBlockchainService {

	private static HFClient client = null;

	private static Channel channel = InitChannel();

	public static void main(String[] args) throws Exception {

		String newOwner = "2000";
		System.out.println("New owner is '" + newOwner + "'\n");

		queryFabcar(channel, "CAR1");
		updateCarOwner(channel, "CAR1", newOwner, false);

		System.out.println("after request for transaction without commit");
		queryFabcar(channel, "CAR1");
		updateCarOwner(channel, "CAR1", newOwner, true);

		System.out.println("after request for transaction WITH commit");
		queryFabcar(channel, "CAR1");

		System.out.println("Sleeping 5s");
		Thread.sleep(5000); // 5secs
		queryFabcar(channel, "CAR1");
		System.out.println("all done");

	}

	private static Channel InitChannel() {
		try {
			client = HFClient.createNewInstance();
			CryptoSuite cs = CryptoSuite.Factory.getCryptoSuite();
			client.setCryptoSuite(cs);

			User user = new SampleUser("c:\\data\\creds", "PeerAdmin");
			// "Log in"
			client.setUserContext(user);
			Channel channel = client.newChannel("mychannel");
			channel.addPeer(client.newPeer("peer", "grpc://192.168.99.100:7051"));
			// It always wants orderer, otherwise even query does not work
			channel.addOrderer(client.newOrderer("orderer", "grpc://192.168.99.100:7050"));
			channel.initialize();
		}
		catch (Exception ex){
		ex.printStackTrace();
		}
		return channel;
	}

	private static void queryFabcar(Channel channel, String key) throws Exception {
		QueryByChaincodeRequest req = client.newQueryProposalRequest();
		ChaincodeID cid = ChaincodeID.newBuilder().setName("fabcar").build();
		req.setChaincodeID(cid);
		req.setFcn("queryCar");
		req.setArgs(new String[] { key });
		System.out.println("Querying for " + key);
		Collection<ProposalResponse> resps = channel.queryByChaincode(req);
		for (ProposalResponse resp : resps) {
			String payload = new String(resp.getChaincodeActionResponsePayload());
			System.out.println("response: " + payload);
		}

	}



	private String getPrnString(ProposalResponse prop){
				String responseStr = "NONE";
				try{
					responseStr = new String(prop.getChaincodeActionResponsePayload());
				}catch (Exception e){
					e.printStackTrace();
				}
				return responseStr;
	}
//Use the url as http://localhost:8080/queryFabcar/car
	@RequestMapping(value = "queryFabcar/{key}", method= RequestMethod.GET)
	public List<String> queryFabcar(@PathVariable String key) throws Exception {
		QueryByChaincodeRequest req = client.newQueryProposalRequest();
		ChaincodeID cid = ChaincodeID.newBuilder().setName("fabcar").build();
		req.setChaincodeID(cid);
		req.setFcn("queryCar");
		req.setArgs(new String[] { key });
		System.out.println("Querying for " + key);
		List sampleOut = Arrays.asList(key,"Ravi","Hackathon Rocks", "BlockChain is Future");
//		Collection<ProposalResponse> resps = channel.queryByChaincode(req);
//		return resps.stream().map(res -> getPrnString(res)).collect(Collectors.toList());
		return sampleOut;
	}


	private static void updateCarOwner(Channel channel, String key, String newOwner, Boolean doCommit)
			throws Exception {
		TransactionProposalRequest req = client.newTransactionProposalRequest();
		ChaincodeID cid = ChaincodeID.newBuilder().setName("fabcar").build();
		req.setChaincodeID(cid);
		req.setFcn("changeCarOwner");
		req.setArgs(new String[] { key });
		System.out.println("Executing for " + key);
		Collection<ProposalResponse> resps = channel.sendTransactionProposal(req);
	    Iterator<ProposalResponse> it = resps.iterator();
	    while(it.hasNext()) {
	    	System.out.println("-------------------");
	    	System.out.println("-------------------");
	    	System.out.println(it.next().isVerified());
	    	System.out.println("-------------------");
	    	System.out.println("-------------------");
	    }
		if (doCommit) {
			// channel.sendTransaction(resps);
			CompletableFuture<TransactionEvent> event = channel.sendTransaction(resps);
			//System.out.println(event.isCompletedExceptionally());
		}
	}

}
