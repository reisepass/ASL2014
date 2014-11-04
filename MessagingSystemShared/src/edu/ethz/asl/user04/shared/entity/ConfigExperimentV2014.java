package edu.ethz.asl.user04.shared.entity;

import java.io.Serializable;

public class ConfigExperimentV2014 extends ConfigExperimentV2 implements
		Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7643385531989657456L;
	public final int numberOfMessageHandlerThreads;
	public final int numberOfDBConnections;
	public final int numberOfMiddleWareMachines;
	public final int numberOfClientMachines;
	public final int numQueues;
	public final double dbFillLevelPerQueue;
	public final int messageLength;
	public final long experimentStartTime; // in Epoch Mills This should be set
											// automatically
	public final long experimentLength; // In milliseconds
	public final String description;
	public final int num_clients;

	public ConfigExperimentV2014() {
		super();
		this.numberOfMessageHandlerThreads = -1;
		this.numberOfDBConnections = -1;
		this.numberOfMiddleWareMachines = -1;
		this.numQueues = -1;
		this.numberOfClientMachines = -1;
		this.dbFillLevelPerQueue = -1;
		this.messageLength = -1;
		this.experimentStartTime = System.currentTimeMillis();
		this.experimentLength = 1000 * 120;
		this.description = "DEBUG_Unknown";
		this.num_clients = -1;


	}

	// TODO make a constructor that takes a single string as input

	public ConfigExperimentV2014(int numberOfMessageHandlerThreads,
			int numberOfDBConnections, int numberOfMiddleWareMachines,
			int numberOfClientMachines,  int numQueues,
			double dbFillLevelPerQueue, int messageLength,
			long experimentLength, String description, int num_clients) {
		super();
		this.numberOfMessageHandlerThreads = numberOfMessageHandlerThreads;
		this.numberOfDBConnections = numberOfDBConnections;
		this.numberOfMiddleWareMachines = numberOfMiddleWareMachines;
		this.numberOfClientMachines = numberOfClientMachines;
		this.numQueues = numQueues;
		this.dbFillLevelPerQueue = dbFillLevelPerQueue;
		this.messageLength = messageLength;
		this.experimentStartTime = System.currentTimeMillis();
		this.experimentLength = experimentLength;
		this.description = description;
		this.num_clients = num_clients;
	}

	@Override
	public String toString() {
		return "#$@!@ConfigExperimentV2 [numberOfMessageHandlerThreads="
				+ numberOfMessageHandlerThreads + ", numberOfDBConnections="
				+ numberOfDBConnections + ", numberOfMiddleWareMachines="
				+ numberOfMiddleWareMachines + " numberOfClientMachines="
				+ numberOfClientMachines + ", numberSafeQueues="
				+ numberSafeQueues + ", num2WayMessage=" + num2WayMessage
				+ ", numSendMessageOnly=" + numSendMessageOnly
				+ ", numPullMessage=" + numPullMessage + ", numPeekMessage="
				+ numPeekMessage + ", numRealisticMixture="
				+ numRealisticMixture + ", numSendMultiMessage="
				+ numSendMultiMessage + ", numReadAllRelevant="
				+ numReadAllRelevant + ", dbFillLevelPerQueue="
				+ dbFillLevelPerQueue + ", messageLength=" + messageLength
				+ ", experimentStartTime=" + experimentStartTime
				+ ", experimentLength=" + experimentLength + "]";
	}

}
