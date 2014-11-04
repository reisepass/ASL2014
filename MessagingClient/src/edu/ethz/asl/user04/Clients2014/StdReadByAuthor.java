package edu.ethz.asl.user04.Clients2014;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

import edu.ethz.asl.user04.clientAPI.MessageAPI2014;
import edu.ethz.asl.user04.shared.entity.ConfigExperimentV2014;
import edu.ethz.asl.user04.shared.entity.Message;
import edu.ethz.user04.shared.requests.queuerequests.CreateClientRequest;
import edu.ethz.user04.shared.requests.queuerequests.ReadSpecificSender;

public class StdReadByAuthor extends StdClient implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int clientID;
	int authorID;
	ArrayList<Integer> authors; 
	MessageAPI2014 capi;
	ConfigExperimentV2014 cfg;
	int queueID;
	boolean removeAfterPeek;

	int sleepBeforeConnectionRetry = 1000;
	int resultChunkSize = 50;

	public StdReadByAuthor(int clientID,  MessageAPI2014 capi,
			ConfigExperimentV2014 cfg, int queueID, int authorID, boolean removeAfterPeek) {
		super();
		this.clientID = clientID;
		this.capi = capi;
		this.cfg = cfg;
		this.queueID = queueID;
		this.authorID = authorID;
		this.removeAfterPeek = removeAfterPeek;

	}
	public StdReadByAuthor(int clientID,  MessageAPI2014 capi,
			ConfigExperimentV2014 cfg, int queueID, ArrayList<Integer> authors, boolean removeAfterPeek) {
		super();
		this.clientID = clientID;
		this.capi = capi;
		this.cfg = cfg;
		this.queueID = queueID;
		this.authorID = -1;
		this.authors = authors;
		this.removeAfterPeek = removeAfterPeek;

	}

	@Override
	public void run() {
		capi.createClient(new CreateClientRequest(this.clientID));
		Random dice = new Random();
		while ((cfg.experimentLength + cfg.experimentStartTime) > System
				.currentTimeMillis()) {
		
			if(authors!=null ){
				authorID = authors.get(dice.nextInt(authors.size()));
			}
			ReadSpecificSender wrq = new ReadSpecificSender(this.clientID, this.queueID, removeAfterPeek, true, authorID);
			wrq.setCFG(cfg);

			//System.out.println("SendingMsg" + clientID);
			Message worked = capi.readSpecSender(wrq);
			if (worked==null ) {
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
