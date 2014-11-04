package edu.ethz.asl.user04.shared.entity;

import java.io.Serializable;
import java.util.HashMap;

/**
 * This class defines how experiments will work. 
 * And will later be used in the logs to determin from which experiment This log comes from. 
 * @author mort
 *
 */
public class ExperimentConfiguration implements Serializable {

		/**
	 * 
	 */
	private static final long serialVersionUID = 5480972318774414312L;
		public final int numberOfMessageHandlerThreads;
		public final int numberOfDBConnections;
		public final int numberOfMiddleWareMachines;
		public final int numberSafeQueues;//This are queues which 
		public final int numberUnsafeQueues;// These may not exist may be deleted or created at random
		public HashMap<String, Integer> numberOfClientsPerType;
		public HashMap<String, Long>  sleepTimeBetweenActionsPerType;
		public HashMap<String, Client>  theActualClientObjectByName;
		
		
		public ExperimentConfiguration(int numberOfMessageHandlerThreads,
				int numberOfDBConnections, int numberOfMiddleWareMachines, int numberSafeQueues,int numberUnsafeQueues) {
			super();
			this.numberOfMessageHandlerThreads = numberOfMessageHandlerThreads;
			this.numberOfDBConnections = numberOfDBConnections;
			this.numberOfMiddleWareMachines = numberOfMiddleWareMachines;
			this.numberSafeQueues = numberSafeQueues;
			this.numberUnsafeQueues = numberUnsafeQueues;
		}
		
		public boolean addClient(Client client, Integer numberOfTheseClient){
			if(theActualClientObjectByName.containsKey(client.getUniqueName())){
				return false;
			}
			else{
				numberOfClientsPerType.put(client.getUniqueName(),numberOfTheseClient);
				sleepTimeBetweenActionsPerType.put(client.getUniqueName(), client.getWaitMilSecBetweenActions());
				theActualClientObjectByName.put(client.getUniqueName(), client);
				return true;
			}
			
		}
	
}
