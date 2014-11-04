package edu.ethz.asl.user04.Clients2014;

import java.util.ArrayList;
import java.util.Random;

import edu.ethz.asl.user04.clientAPI.MessageAPI2014;
import edu.ethz.asl.user04.shared.entity.ConfigExperimentV2014;
import edu.ethz.user04.shared.requests.queuerequests.CreateClientRequest;
import edu.ethz.user04.shared.requests.queuerequests.QueryForQueuesWithMessagesForMe;

public class StdFindRelQue extends StdClient {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	int clientID;
	MessageAPI2014 capi;
	ConfigExperimentV2014 cfg;


	int sleepBeforeConnectionRetry = 1000;
	int resultChunkSize = 50;

	public StdFindRelQue(int clientID,  MessageAPI2014 capi,
			ConfigExperimentV2014 cfg) {
		super();
		this.clientID = clientID;
		this.capi = capi;
		this.cfg = cfg;

	}
	
	@Override
	public void run() {
		capi.createClient(new CreateClientRequest(this.clientID));
		Random dice = new Random();
		while ((cfg.experimentLength + cfg.experimentStartTime) > System
				.currentTimeMillis()) {
			QueryForQueuesWithMessagesForMe qq = new QueryForQueuesWithMessagesForMe(clientID);
			ArrayList<Integer> queuesFound =  capi.getQueuesWithRelevantMessages(qq);

			if (queuesFound==null ) {
				try {
					Thread.sleep(100);
					// TODO SEND Error TO Log
				} catch (InterruptedException e) {

					e.printStackTrace();
				}
			}
		}
		System.out.println(" Finished Client "+clientID);
	}
	
}
