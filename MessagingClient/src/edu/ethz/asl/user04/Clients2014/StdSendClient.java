package edu.ethz.asl.user04.Clients2014;

import java.io.Serializable;
import java.util.Random;

import edu.ethz.asl.user04.clientAPI.MessageAPI2014;
import edu.ethz.asl.user04.shared.entity.ConfigExperimentV2014;
import edu.ethz.asl.user04.shared.entity.Message;
import edu.ethz.user04.shared.requests.messagerequests.WriteMessageRequest;
import edu.ethz.user04.shared.requests.queuerequests.CreateClientRequest;

public class StdSendClient extends StdClient implements Serializable, Runnable {
	/**
	 * FUN FUN FUN FUN
	 */
	private static final long serialVersionUID = -1784111427520114075L;

	int clientID;
	String message;
	MessageAPI2014 capi;
	ConfigExperimentV2014 cfg;
	int queueID;
	int messageLength;
	int sleepBeforeConnectionRetry = 1000;
	int resultChunkSize = 50;

	public StdSendClient(int clientID, int messageLength, MessageAPI2014 capi,
			ConfigExperimentV2014 cfg, int queueID) {
		super();
		this.clientID = clientID;
		this.messageLength = messageLength;
		this.capi = capi;
		this.cfg = cfg;
		this.queueID = queueID;

	}

	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	static Random rnd = new Random();

	String randomString(int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++)
			sb.append(AB.charAt(rnd.nextInt(AB.length())));
		return sb.toString();
	}

	@Override
	public void run() {
		capi.createClient(new CreateClientRequest(this.clientID));
		while ((cfg.experimentLength + cfg.experimentStartTime) > System
				.currentTimeMillis()) {
			message = randomString(this.messageLength);
			Message prepMessage = new Message(clientID, queueID, message);
			WriteMessageRequest wrq = new WriteMessageRequest(prepMessage);

			wrq.setCFG(cfg);

			//System.out.println("SendingMsg" + clientID);
			boolean worked = capi.sendMessage(wrq);
			if (!worked) {
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
