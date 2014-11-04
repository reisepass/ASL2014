package edu.ethz.asl.user04.messagebroker;

public class ConfigurableMessageBrokerService extends MessageBrokerService {
	protected  int HANDLERS ;
	protected  int CONNECTIONLIMIT;//100 is the postgres internal limit and it needs some for itself
	protected static final int SERVER_PORT = 5009;
	public ConfigurableMessageBrokerService( int numMessageHandlers, int numDBConnections){
		HANDLERS=numMessageHandlers;
		CONNECTIONLIMIT=numDBConnections;
		
	}
}
