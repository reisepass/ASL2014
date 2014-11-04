package edu.ethz.asl.user04.clientAPI;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.ethz.asl.user04.dbutils.DBManager;
import edu.ethz.asl.user04.dbutils.SQLUtil_v2014;
import edu.ethz.asl.user04.shared.entity.ConfigExperimentV2014;
import edu.ethz.asl.user04.shared.entity.Message;
import edu.ethz.asl.user04.shared.entity.RequestResponse;
import edu.ethz.asl.user04.shared.entity.StatTrack;
import edu.ethz.asl.user04.shared.logging.MessagingSystemLogger;
import edu.ethz.user04.shared.requests.messagerequests.DeleteMessageRequest;
import edu.ethz.user04.shared.requests.messagerequests.MessagingSystemRequest;
import edu.ethz.user04.shared.requests.messagerequests.WriteMessageRequest;
import edu.ethz.user04.shared.requests.queuerequests.CloseConnection;
import edu.ethz.user04.shared.requests.queuerequests.CreateClientRequest;
import edu.ethz.user04.shared.requests.queuerequests.CreateQueueRequest;
import edu.ethz.user04.shared.requests.queuerequests.DeleteQueueRequest;
import edu.ethz.user04.shared.requests.queuerequests.QueryForQueuesWithMessagesForMe;
import edu.ethz.user04.shared.requests.queuerequests.ReadAllMessagesRequest;
import edu.ethz.user04.shared.requests.queuerequests.ReadPrivateMessageRequest;
import edu.ethz.user04.shared.requests.queuerequests.ReadSpecificSender;


public class MessageAPI2014 implements ClientAPIInterface {
	
	public final static Logger LOGGER = MessagingSystemLogger.getLoggerForClass(MessageAPI2014.class.getName());
	
	Socket socket;
	ObjectOutputStream outputStream;
	ObjectInputStream  inputStream;
	int tooManyMessages=10000;
	int clientId; 
	int NUMBER_OF_RR_POLLS = 5; // Number of times to poll for Response in Req-Resp communication
	boolean isRequestResponse = false; // Client is One-way by default
	int context = 0; // Default context is 0 (= Broadcast messages)
	public boolean debugModeOn=false;
	private static final int DEFAULT_SERVER_PORT = 5009;
	private static final String DEFAULT_SERVER_URL = "127.0.0.1";
	public ConfigExperimentV2014 cfg;
	
	int sleepBeforeConnectionRetry = 1000;
	int resultChunkSize = 50;

	
	String brokerServiceURL;
	int port;
	
	public MessageAPI2014( ) throws UnknownHostException, IOException{
		this.brokerServiceURL=DEFAULT_SERVER_URL;
		this.port=DEFAULT_SERVER_PORT;
		cfg=null;
	}
	
	public MessageAPI2014( String brokerServiceURL, int port) throws UnknownHostException, IOException{
		this.brokerServiceURL=brokerServiceURL;
		this.port=port;
		cfg=null;
	}
	
	// OT - If this is enabled, the message handler keeps listening for objects.
	// Message Handler terminates only when it receives a CloseConnectionObject
	// This should match BATCH_REQUExSTS in MessageHandler
	// Maybe read this from config?
	private final static boolean BATCH_REQUESTS = false;
	/**
	 * Returns an instance of ClientAPI for client to perform functions
	 * MessageBrokerService URL defaults to 127.0.0.1
	 * MessageBrokerService Port defaults to 5009
	 * @param clientID Client ID
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public MessageAPI2014(int clientID) throws UnknownHostException, IOException {
		this(clientID, DEFAULT_SERVER_URL); //TODO Check if this can cause a problem. 
		cfg=null;
	}
	
	/**
	 * Returns an instance of ClientAPI for client to perform functions
	 * MessageBrokerService Port defaults to 5009
	 * @param clientID Client ID
	 * @param brokerServiceURL URL/DNS for the MessageBrokerService. Default is 127.0.0.1
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public MessageAPI2014(int clientID, String brokerServiceURL) throws UnknownHostException, IOException {
		this(clientID, brokerServiceURL, DEFAULT_SERVER_PORT);
		cfg=null;
	}
	
	/**
	 * Returns an instance of ClientAPI for client to perform functions
	 * MessageBrokerService Port defaults to 5009
	 * @param clientID Client ID
	 * @param brokerServiceURL URL/DNS for the MessageBrokerService. Default is 127.0.0.1
	 * @param port Post open at the Server. Default is 5009
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public MessageAPI2014(int clientID, String brokerServiceURL, int port) throws UnknownHostException, IOException {
		this.clientId = clientID;
		this.brokerServiceURL = brokerServiceURL;
		this.port = port;
		
		cfg=null;
		// socket = new Socket(brokerServiceURL, 5009);
		// outputStream = new ObjectOutputStream(socket.getOutputStream());
		// inputStream = new ObjectInputStream( socket.getInputStream());
	}
	
	public MessageAPI2014(int clientID, String brokerServiceURL, int port, ConfigExperimentV2014 cfg) throws UnknownHostException, IOException {
		this.clientId = clientID;
		this.brokerServiceURL = brokerServiceURL;
		this.port = port;
		
		this.cfg = cfg;
		
	}
	

	
	
	private void initConnection(){//TODO force retry after certan sleep period
		try {
			socket = new Socket(brokerServiceURL, port);
			outputStream = new ObjectOutputStream(socket.getOutputStream());
			inputStream = new ObjectInputStream( socket.getInputStream()); //TODO why new object ??
		} catch (UnknownHostException e) {
			LOGGER.log(Level.SEVERE,"MessageAPI2014 unable to connect to the Middleware",e);
			e.printStackTrace();
			
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE,"MessageAPI2014 unable to connect to the Middleware",e);
			e.printStackTrace();
		}
		
		
	}
	
	
	
	
	

	
	
	@Override
	public void setCliendID(int clientId) {
		this.clientId = clientId;
	}

	@Override
	public void enableRequestResponseMode() {
		this.isRequestResponse = true;
		//this.context = context;
	}

	@Override
	public void disableRequestResponseMode() {
		this.isRequestResponse = false;
		//this.context = 0;
	}
	
	@Override
	public boolean sendMessage(WriteMessageRequest writeMessageRequest) {
		
		StatTrack clTimes = new StatTrack();
		clTimes.clStarts = System.currentTimeMillis();
				
		
		Socket tempSocket = null;
		ObjectOutputStream outputStream = null;
		ObjectInputStream inputStream = null;

		
		
		
		RequestResponse resp = new RequestResponse(false, null);
		writeMessageRequest.message.setSenderId(this.clientId);
		
		try {
		
			
			tempSocket = new Socket(brokerServiceURL, port);
			
			
			
			inputStream = new ObjectInputStream(tempSocket.getInputStream());
			outputStream = new ObjectOutputStream(tempSocket.getOutputStream());
			
			clTimes.clWaitsinMWQ = System.currentTimeMillis();
			//Wait for ready Thread
			inputStream.readObject();  // //This is a simple flag sent from the Middle ware as its first action once a runnalbe thread has picked this clients socket from the Queue. This is primarily here for benchmarking the time this client spends in the Queue
			clTimes.clOutofMWQ = System.currentTimeMillis();
			// The additional time recorded for transferring this boolean object can be corrected for by checking measuring all the times when the system appeared to have more clients in the Queue than is possible. And then simply subtracting out this time because it is the over lap
			
			// Write Request object
			clTimes.clGotOutStream = System.currentTimeMillis();
			outputStream.writeObject(writeMessageRequest);
			clTimes.clSentReqToMW= System.currentTimeMillis();
			
			// Read Response object			
			resp = (RequestResponse) inputStream.readObject();
			clTimes.clRespFromMW = System.currentTimeMillis();

			
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "SendMessage failed for RequestID "
					+ writeMessageRequest.getRequestUUID(), e);
		}
		
		try {
			
			outputStream.close();
			inputStream.close();
			tempSocket.close();
			clTimes.clClosedConn = System.currentTimeMillis();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to close socket"
					+ writeMessageRequest.getRequestUUID(), e);
		}
		clTimes.clClosedConn = System.currentTimeMillis();

		
		
		StatTrack outTimes = clTimes.merge(resp.timing);
		outTimes.comput();
		LOGGER.log(
				Level.INFO,
				String.format(
						"[CSVFRMT]  %s, %d, %d, %d, %d, %d, %d, %d, %d, %s ,%d ,%s, %d, %d, %d, %d, %d, %d, %s, %d, %d, %s, %d ",
						cfg.description,
						cfg.experimentStartTime,
						cfg.experimentLength,
						cfg.numberOfMessageHandlerThreads,
						cfg.numberOfMiddleWareMachines,
						cfg.numberOfDBConnections,
						cfg.numberOfClientMachines,
						cfg.numQueues,
						cfg.num_clients,
						writeMessageRequest.getRequestUUID(),
						this.clientId,
						writeMessageRequest.message.getReceiverId()<=0  ? "sendMpub" : "sendMpriv",
						outTimes.clThinkTime,
						outTimes.clRoundTime,
						outTimes.mwThinkTime,
						outTimes.mwRoundTime,
						outTimes.dbRoundTime,
						outTimes.dbThinkTime,
						resp.success ? "[s]" : "[f]",				
						outTimes.mwTimeInDBQ,
						outTimes.clTimeInQ,
						"Empty",
						1
						
						)
						+String.format(", %d, %d, %d, %d, %d, %d, %d, %d, %d",
								outTimes.clWaitsinMWQ-outTimes.clStarts, //Connection init time 
								outTimes.clClosedConn-outTimes.clRespFromMW, // Close connection time
								outTimes.mwGetsStreams-outTimes.mwStarts, //Open stream time on mw
								outTimes.mwSentReadyToClient-outTimes.mwGetsStreams, //Time to send ready Message
								outTimes.mwWaitsinDBQ-outTimes.mwSentReadyToClient, //Time to receive Request from client 
								outTimes.mwWaitsinDBQ-cfg.experimentStartTime,  //used for q size calc
								outTimes.mwOutofDBQ-cfg.experimentStartTime, //used for q size calc
								outTimes.clWaitsinMWQ-cfg.experimentStartTime,   //used for q size calc
								outTimes.clOutofMWQ-cfg.experimentStartTime  //used for q size calc
						)
						+String.format(", %d, %d, %d, %d, %d, %d",
								outTimes.qSizeDB,
								outTimes.qSizeMW,
								outTimes.mwNoQRound,
								outTimes.mwNetworkTime,
								outTimes.mwID,
								outTimes.mwReadyNetTime
						)
				);

		return resp.success;
	}

	// context is the context in the message and not the context in this class
	@Override
	public Message sendRequestResponseMessage(
			WriteMessageRequest writeMessageRequest, long timeout) {

		return null;

	}

	
	@Override
	public List<Message> readMultipleMessages(
			ReadAllMessagesRequest readAllRequest) {
		
		return null;
	}
	
public boolean createClient(CreateClientRequest createClientRequest) {
		
		StatTrack clTimes = new StatTrack();
		clTimes.clStarts = System.currentTimeMillis();
				
		
		Socket tempSocket = null;
		ObjectOutputStream outputStream = null;
		ObjectInputStream inputStream = null;

		
		
		
		RequestResponse resp = new RequestResponse(false, null);
		
		
		try {
		
			
			tempSocket = new Socket(brokerServiceURL, port);
			
			
			
			inputStream = new ObjectInputStream(tempSocket.getInputStream());
			outputStream = new ObjectOutputStream(tempSocket.getOutputStream());
			
			clTimes.clWaitsinMWQ = System.currentTimeMillis();
			//Wait for ready Thread
			inputStream.readObject();  // //This is a simple flag sent from the Middle ware as its first action once a runnalbe thread has picked this clients socket from the Queue. This is primarily here for benchmarking the time this client spends in the Queue
			clTimes.clOutofMWQ = System.currentTimeMillis();
			// The additional time recorded for transferring this boolean object can be corrected for by checking measuring all the times when the system appeared to have more clients in the Queue than is possible. And then simply subtracting out this time because it is the over lap
			
			// Write Request object
			clTimes.clGotOutStream = System.currentTimeMillis();
			outputStream.writeObject(createClientRequest);
			clTimes.clSentReqToMW= System.currentTimeMillis();
			
			// Read Response object			
			resp = (RequestResponse) inputStream.readObject();
			clTimes.clRespFromMW = System.currentTimeMillis();

			
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "SendMessage failed for RequestID "
					+ createClientRequest.getRequestUUID(), e);
		}
		
		try {
			
			outputStream.close();
			inputStream.close();
			tempSocket.close();
			clTimes.clClosedConn = System.currentTimeMillis();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to close socket"
					+ createClientRequest.getRequestUUID(), e);
		}
		clTimes.clClosedConn = System.currentTimeMillis();

		
		
		StatTrack outTimes = clTimes.merge(resp.timing);
		outTimes.comput();
		LOGGER.log(
				Level.INFO,
				String.format(
						"[CSVFRMT]  %s, %d, %d, %d, %d, %d, %d, %d, %d, %s ,%d ,%s, %d, %d, %d, %d, %d, %d, %s, %d, %d, %s, %d ",
						cfg.description,
						cfg.experimentStartTime,
						cfg.experimentLength,
						cfg.numberOfMessageHandlerThreads,
						cfg.numberOfMiddleWareMachines,
						cfg.numberOfDBConnections,
						cfg.numberOfClientMachines,
						cfg.numQueues,
						cfg.num_clients,
						createClientRequest.getRequestUUID(),
						this.clientId,
                        "createUser",
						outTimes.clThinkTime,
						outTimes.clRoundTime,
						outTimes.mwThinkTime,
						outTimes.mwRoundTime,
						outTimes.dbRoundTime,
						outTimes.dbThinkTime,
						resp.success ? "[s]" : "[f]",				
						outTimes.mwTimeInDBQ,
						outTimes.clTimeInQ,
						"Empty",
						1
						
						)
						+String.format(", %d, %d, %d, %d, %d, %d, %d, %d, %d",
								outTimes.clWaitsinMWQ-outTimes.clStarts, //Connection init time 
								outTimes.clClosedConn-outTimes.clRespFromMW, // Close connection time
								outTimes.mwGetsStreams-outTimes.mwStarts, //Open stream time on mw
								outTimes.mwSentReadyToClient-outTimes.mwGetsStreams, //Time to send ready Message
								outTimes.mwWaitsinDBQ-outTimes.mwSentReadyToClient, //Time to receive Request from client 
								outTimes.mwWaitsinDBQ-cfg.experimentStartTime,  //used for q size calc
								outTimes.mwOutofDBQ-cfg.experimentStartTime, //used for q size calc
								outTimes.clWaitsinMWQ-cfg.experimentStartTime,   //used for q size calc
								outTimes.clOutofMWQ-cfg.experimentStartTime  //used for q size calc
						)+String.format(", %d, %d, %d, %d, %d, %d",
								outTimes.qSizeDB,
								outTimes.qSizeMW,
								outTimes.mwNoQRound,
								outTimes.mwNetworkTime,
								outTimes.mwID,
								outTimes.mwReadyNetTime
						)
				);

		return resp.success;
	}
	
	@Override
	public Message readOnePrivateMessage(ReadPrivateMessageRequest readMessageRequest) {
		
		StatTrack clTimes = new StatTrack();
		clTimes.clStarts = System.currentTimeMillis();
			
		
		
		Socket tempSocket = null;
		ObjectOutputStream outputStream = null;
		ObjectInputStream inputStream = null;
		RequestResponse resp  = new RequestResponse(false, null);
		Message message = null;
		
		readMessageRequest.setReceiverID(this.clientId);
		
		try {
			tempSocket = new Socket(brokerServiceURL, port);

			// Write Request object
			inputStream = new ObjectInputStream(tempSocket.getInputStream());
			outputStream = new ObjectOutputStream(tempSocket.getOutputStream());
			clTimes.clWaitsinMWQ = System.currentTimeMillis();
			inputStream.readObject(); //This is a simple flag sent from the Middle ware as its first action once a runnalbe thread has picked this clients socket from the Queue. This is primarily here for benchmarking the time this client spends in the Queue
			clTimes.clOutofMWQ =  System.currentTimeMillis();
			
			clTimes.clGotOutStream = System.currentTimeMillis();
			outputStream.writeObject(readMessageRequest);
			clTimes.clSentReqToMW = System.currentTimeMillis();
			
			
			resp = (RequestResponse) inputStream.readObject();
			clTimes.clRespFromMW = System.currentTimeMillis(); 
			message = (Message) resp.getPayload();
			
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "SendMessage failed for RequestID"
					+ readMessageRequest.getRequestUUID(), e);
		}
		
		try {
			outputStream.close();
			inputStream.close();
			tempSocket.close();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to close socket "
					+ readMessageRequest.getRequestUUID(), e);
		}
		clTimes.clClosedConn = System.currentTimeMillis();

		StatTrack outTimes = clTimes.merge(resp.timing);
		outTimes.comput();
		LOGGER.log(
				Level.INFO,
				String.format(
						"[CSVFRMT]  %s, %d, %d, %d, %d, %d, %d, %d, %d, %s ,%d ,%s, %d, %d, %d, %d, %d, %d, %s, %d, %d, %s, %d ",
						cfg.description,
						cfg.experimentStartTime,
						cfg.experimentLength,
						cfg.numberOfMessageHandlerThreads,
						cfg.numberOfMiddleWareMachines,
						cfg.numberOfDBConnections,
						cfg.numberOfClientMachines,
						cfg.numQueues,
						cfg.num_clients,
						readMessageRequest.getRequestUUID(),
						this.clientId,
						readMessageRequest.removeAfterPeek ? "readPop" : "readPull",
						outTimes.clThinkTime,
						outTimes.clRoundTime,
						outTimes.mwThinkTime,
						outTimes.mwRoundTime,
						outTimes.dbRoundTime,
						outTimes.dbThinkTime,
						resp.success ? "[s]" : "[f]",							
						outTimes.mwTimeInDBQ,
						outTimes.clTimeInQ,
						"FoundOne",
						1       //This still needs to be be implemetned inside SQLUtil  because ATM we dont know the dif between an SQL failure and empty queues
						)
						+String.format(", %d, %d, %d, %d, %d, %d, %d, %d, %d",
								outTimes.clWaitsinMWQ-outTimes.clStarts, //Connection init time 
								outTimes.clClosedConn-outTimes.clRespFromMW, // Close connection time
								outTimes.mwGetsStreams-outTimes.mwStarts, //Open stream time on mw
								outTimes.mwSentReadyToClient-outTimes.mwGetsStreams, //Time to send ready Message
								outTimes.mwReceived-outTimes.mwSentReadyToClient, //Time to receive Request from client 
								outTimes.mwWaitsinDBQ-cfg.experimentStartTime,  //used for q size calc
								outTimes.mwOutofDBQ-cfg.experimentStartTime, //used for q size calc
								outTimes.clWaitsinMWQ-cfg.experimentStartTime,   //used for q size calc
								outTimes.clOutofMWQ-cfg.experimentStartTime  //used for q size calc
						)+String.format(", %d, %d, %d, %d, %d, %d",
								outTimes.qSizeDB,
								outTimes.qSizeMW,
								outTimes.mwNoQRound,
								outTimes.mwNetworkTime,
								outTimes.mwID,
								outTimes.mwReadyNetTime
						)
				);
		
	
		return message;
	}
	
	


	
	@Override
	public boolean createQueue(CreateQueueRequest createQueueRequest) {
		StatTrack clTimes = new StatTrack();
		clTimes.clStarts = System.currentTimeMillis();
		
		RequestResponse response = new RequestResponse(false, null);
		Socket tempSocket = null;
		ObjectOutputStream outputStream = null;
		ObjectInputStream inputStream = null;
		
		createQueueRequest.setClientid(this.clientId);
		
		try {
			tempSocket = new Socket(brokerServiceURL, port);

			// Write Request object
			inputStream = new ObjectInputStream(tempSocket.getInputStream());
			outputStream = new ObjectOutputStream(tempSocket.getOutputStream());
			clTimes.clWaitsinMWQ= System.currentTimeMillis();
			inputStream.readObject(); //This is a simple flag sent from the Middle ware as its first action once a runnalbe thread has picked this clients socket from the Queue. This is primarily here for benchmarking the time this client spends in the Queue
			clTimes.clOutofMWQ = System.currentTimeMillis();
			
			outputStream.writeObject(createQueueRequest);
			clTimes.clSentReqToMW = System.currentTimeMillis();
			// Read Response object

			
			response = (RequestResponse) inputStream.readObject();
			clTimes.clRespFromMW = System.currentTimeMillis();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "SendMessage failed for RequestID"
					+ createQueueRequest.getRequestUUID(), e);
		}
		
		try {
			outputStream.close();
			inputStream.close();
			tempSocket.close();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to close socket "
					+ createQueueRequest.getRequestUUID(), e);
		}
		clTimes.clClosedConn = System.currentTimeMillis();
		StatTrack outTimes = clTimes.merge(response.timing);
		outTimes.comput();
		LOGGER.log(
				Level.INFO,
				String.format(
						"[CSVFRMT]  %s, %d, %d, %d, %d, %d, %d, %d, %d, %s ,%d ,%s, %d, %d, %d, %d, %d, %d, %s, %d, %d, %s, %d ",
						cfg.description,
						cfg.experimentStartTime,
						cfg.experimentLength,
						cfg.numberOfMessageHandlerThreads,
						cfg.numberOfMiddleWareMachines,
						cfg.numberOfDBConnections,
						cfg.numberOfClientMachines,
						cfg.numQueues,
						cfg.num_clients,
						createQueueRequest.getRequestUUID(),
						this.clientId,
						"createQueueRequest",
						outTimes.clThinkTime,
						outTimes.clRoundTime,
						outTimes.mwThinkTime,
						outTimes.mwRoundTime,
						outTimes.dbRoundTime,
						outTimes.dbThinkTime,
						response.success ? "[s]" : "[f]",							
						outTimes.mwTimeInDBQ,
						outTimes.clTimeInQ,
						"Empty",
						1
						)
						+String.format(", %d, %d, %d, %d, %d, %d, %d, %d, %d",
								outTimes.clWaitsinMWQ-outTimes.clStarts, //Connection init time 
								outTimes.clClosedConn-outTimes.clRespFromMW, // Close connection time
								outTimes.mwGetsStreams-outTimes.mwStarts, //Open stream time on mw
								outTimes.mwSentReadyToClient-outTimes.mwGetsStreams, //Time to send ready Message
								outTimes.mwWaitsinDBQ-outTimes.mwSentReadyToClient, //Time to receive Request from client 
								outTimes.mwWaitsinDBQ-cfg.experimentStartTime,  //used for q size calc
								outTimes.mwOutofDBQ-cfg.experimentStartTime, //used for q size calc
								outTimes.clWaitsinMWQ-cfg.experimentStartTime,   //used for q size calc
								outTimes.clOutofMWQ-cfg.experimentStartTime  //used for q size calc
						)+String.format(", %d, %d, %d, %d, %d, %d",
								outTimes.qSizeDB,
								outTimes.qSizeMW,
								outTimes.mwNoQRound,
								outTimes.mwNetworkTime,
								outTimes.mwID,
								outTimes.mwReadyNetTime
						)
				);
		
	
		
		return response.success;
	}

	
	@Override
	public boolean deleteQueue(DeleteQueueRequest deleteQueueRequest) {
		StatTrack clTimes = new StatTrack();
		clTimes.clStarts = System.currentTimeMillis();

		Boolean response = false;
		Socket tempSocket = null;
		ObjectOutputStream outputStream = null;
		ObjectInputStream inputStream = null;
		RequestResponse resp = new RequestResponse(false, null); 
		try {
			tempSocket = new Socket(brokerServiceURL, port);

			inputStream = new ObjectInputStream(tempSocket.getInputStream());
			outputStream = new ObjectOutputStream(tempSocket.getOutputStream());
			clTimes.clWaitsinMWQ = System.currentTimeMillis();
			inputStream.readObject(); //This is a simple flag sent from the Middle ware as its first action once a runnalbe thread has picked this clients socket from the Queue. This is primarily here for benchmarking the time this client spends in the Queue
			clTimes.clOutofMWQ = System.currentTimeMillis();
			clTimes.clGotOutStream = System.currentTimeMillis();
			
			outputStream.writeObject(deleteQueueRequest);
			clTimes.clSentReqToMW = System.currentTimeMillis();
			
			// Read Response object
			
			resp = (RequestResponse) inputStream.readObject();
			clTimes.clRespFromMW = System.currentTimeMillis();
			response = (Boolean) resp.getPayload();
			
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "SendMessage failed for RequestID"
					+ deleteQueueRequest.getRequestUUID(), e);
		}
		
		try {
			outputStream.close();
			inputStream.close();
			tempSocket.close();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to close socket "
					+ deleteQueueRequest.getRequestUUID(), e);
		}
		clTimes.clClosedConn = System.currentTimeMillis();
		StatTrack outTimes = clTimes.merge(resp.timing);
		outTimes.comput();
		LOGGER.log(
				Level.INFO,
				String.format(
						"[CSVFRMT]  %s, %d, %d, %d, %d, %d, %d, %d, %d, %s ,%d ,%s, %d, %d, %d, %d, %d, %d, %s, %d, %d, %s, %d ",
						cfg.description,
						cfg.experimentStartTime,
						cfg.experimentLength,
						cfg.numberOfMessageHandlerThreads,
						cfg.numberOfMiddleWareMachines,
						cfg.numberOfDBConnections,
						cfg.numberOfClientMachines,
						cfg.numQueues,
						cfg.num_clients,
						deleteQueueRequest.getRequestUUID(),
						this.clientId,
						"deleteQueueRequest",
						outTimes.clThinkTime,
						outTimes.clRoundTime,
						outTimes.mwThinkTime,
						outTimes.mwRoundTime,
						outTimes.dbRoundTime,
						outTimes.dbThinkTime,
						resp.success ? "[s]" : "[f]",							
						outTimes.mwTimeInDBQ,
						outTimes.clTimeInQ,
						"Empty",
						1
						)
						+String.format(", %d, %d, %d, %d, %d, %d, %d, %d, %d",
								outTimes.clWaitsinMWQ-outTimes.clStarts, //Connection init time 
								outTimes.clClosedConn-outTimes.clRespFromMW, // Close connection time
								outTimes.mwGetsStreams-outTimes.mwStarts, //Open stream time on mw
								outTimes.mwSentReadyToClient-outTimes.mwGetsStreams, //Time to send ready Message
								outTimes.mwWaitsinDBQ-outTimes.mwSentReadyToClient, //Time to receive Request from client 
								outTimes.mwWaitsinDBQ-cfg.experimentStartTime,  //used for q size calc
								outTimes.mwOutofDBQ-cfg.experimentStartTime, //used for q size calc
								outTimes.clWaitsinMWQ-cfg.experimentStartTime,   //used for q size calc
								outTimes.clOutofMWQ-cfg.experimentStartTime  //used for q size calc
						)+String.format(", %d, %d, %d, %d, %d, %d",
								outTimes.qSizeDB,
								outTimes.qSizeMW,
								outTimes.mwNoQRound,
								outTimes.mwNetworkTime,
								outTimes.mwID,
								outTimes.mwReadyNetTime
						)
				);
		
		
		return response;
	}
	
	
	public ArrayList<Integer> getQueuesWithRelevantMessages(QueryForQueuesWithMessagesForMe queueRequest){
		StatTrack clTimes = new StatTrack();
		clTimes.clStarts = System.currentTimeMillis();
		
		// This method returns null in case something goes wrong.
		ArrayList<Integer> queuesWithRelevantMessages = null;
		RequestResponse response = new RequestResponse(false, null);
		Socket tempSocket = null;
		ObjectOutputStream outputStream = null;
		ObjectInputStream inputStream = null;
		
		queueRequest.setReceiverId(this.clientId);
		
		try {
			tempSocket = new Socket(brokerServiceURL, port);
			inputStream = new ObjectInputStream(tempSocket.getInputStream());
			outputStream = new ObjectOutputStream(tempSocket.getOutputStream());
			clTimes.clWaitsinMWQ= System.currentTimeMillis();
			inputStream.readObject(); //This is a simple flag sent from the Middle ware as its first action once a runnalbe thread has picked this clients socket from the Queue. This is primarily here for benchmarking the time this client spends in the Queue
			clTimes.clOutofMWQ = System.currentTimeMillis();
			// Write Request object
			
			outputStream.writeObject(queueRequest);
			clTimes.clSentReqToMW = System.currentTimeMillis();
			// Read Response object
		
			response = (RequestResponse) inputStream.readObject();
			clTimes.clRespFromMW = System.currentTimeMillis();
			if (response.success) {
				if (response.getPayload() instanceof ArrayList<?>) {
					queuesWithRelevantMessages = (ArrayList<Integer>) response.getPayload();
				} else
					throw new Exception(
							"Unable to cast response for request ID "
									+ queueRequest.getRequestUUID()
									+ ". Expected ArrayList<Integer>, got something else");
			} else {
				LOGGER.warning(String.format("Response for Request %s failed. Returning null.", queueRequest.getRequestUUID()));
			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "SendMessage failed for RequestID"
					+ queueRequest.getRequestUUID(), e);
		}
		
		try {
			outputStream.close();
			inputStream.close();
			tempSocket.close();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to close socket "
					+ queueRequest.getRequestUUID(), e);
		}

		clTimes.clClosedConn = System.currentTimeMillis();
		

		StatTrack outTimes = clTimes.merge(response.timing);
		outTimes.comput();
		LOGGER.log(
				Level.INFO,
				String.format(
						"[CSVFRMT]  %s, %d, %d, %d, %d, %d, %d, %d, %d, %s ,%d ,%s, %d, %d, %d, %d, %d, %d, %s, %d, %d, %s, %d ",
						cfg.description,
						cfg.experimentStartTime,
						cfg.experimentLength,
						cfg.numberOfMessageHandlerThreads,
						cfg.numberOfMiddleWareMachines,
						cfg.numberOfDBConnections,
						cfg.numberOfClientMachines,
						cfg.numQueues,
						cfg.num_clients,
						queueRequest.getRequestUUID(),
						this.clientId,
						"queueRequest",
						outTimes.clThinkTime,
						outTimes.clRoundTime,
						outTimes.mwThinkTime,
						outTimes.mwRoundTime,
						outTimes.dbRoundTime,
						outTimes.dbThinkTime,
						response.success ? "[s]" : "[f]",							
						outTimes.mwTimeInDBQ,
						outTimes.clTimeInQ,
						"numReturned",
						queuesWithRelevantMessages.size()
						)
						+String.format(", %d, %d, %d, %d, %d, %d, %d, %d, %d",
								outTimes.clWaitsinMWQ-outTimes.clStarts, //Connection init time 
								outTimes.clClosedConn-outTimes.clRespFromMW, // Close connection time
								outTimes.mwGetsStreams-outTimes.mwStarts, //Open stream time on mw
								outTimes.mwSentReadyToClient-outTimes.mwGetsStreams, //Time to send ready Message
								outTimes.mwWaitsinDBQ-outTimes.mwSentReadyToClient, //Time to receive Request from client 
								outTimes.mwWaitsinDBQ-cfg.experimentStartTime,  //used for q size calc
								outTimes.mwOutofDBQ-cfg.experimentStartTime, //used for q size calc
								outTimes.clWaitsinMWQ-cfg.experimentStartTime,   //used for q size calc
								outTimes.clOutofMWQ-cfg.experimentStartTime  //used for q size calc
						)+String.format(", %d, %d, %d, %d, %d, %d",
								outTimes.qSizeDB,
								outTimes.qSizeMW,
								outTimes.mwNoQRound,
								outTimes.mwNetworkTime,
								outTimes.mwID,
								outTimes.mwReadyNetTime
						)
				);
		
		return queuesWithRelevantMessages;	
	}

	
	
	
	public Message readSpecSender(ReadSpecificSender readByAuthor) {
		
		StatTrack clTimes = new StatTrack();
		clTimes.clStarts = System.currentTimeMillis();
			
		
		
		Socket tempSocket = null;
		ObjectOutputStream outputStream = null;
		ObjectInputStream inputStream = null;
		RequestResponse resp  = new RequestResponse(false, null);
		Message message = null;
		
		readByAuthor.senderID = this.clientId;
		
		try {
			tempSocket = new Socket(brokerServiceURL, port);

			// Write Request object
			inputStream = new ObjectInputStream(tempSocket.getInputStream());
			outputStream = new ObjectOutputStream(tempSocket.getOutputStream());
			clTimes.clWaitsinMWQ = System.currentTimeMillis();
			inputStream.readObject(); //This is a simple flag sent from the Middle ware as its first action once a runnalbe thread has picked this clients socket from the Queue. This is primarily here for benchmarking the time this client spends in the Queue
			clTimes.clOutofMWQ =  System.currentTimeMillis();
			
			clTimes.clGotOutStream = System.currentTimeMillis();
			outputStream.writeObject(readByAuthor);
			clTimes.clSentReqToMW = System.currentTimeMillis();
			
			
			resp = (RequestResponse) inputStream.readObject();
			clTimes.clRespFromMW = System.currentTimeMillis(); 
			message = (Message) resp.getPayload();
			
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "SendMessage failed for RequestID"
					+ readByAuthor.getRequestUUID(), e);
		}
		
		try {
			outputStream.close();
			inputStream.close();
			tempSocket.close();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to close socket "
					+ readByAuthor.getRequestUUID(), e);
		}
		clTimes.clClosedConn = System.currentTimeMillis();
		if( resp.success) {
			if( message !=null && message.senderId == readByAuthor.auther){ //double checked
				resp.success = true;
				
			}
		}
		

		StatTrack outTimes = clTimes.merge(resp.timing);
		outTimes.comput();
		LOGGER.log(
				Level.INFO,
				String.format(
						"[CSVFRMT]  %s, %d, %d, %d, %d, %d, %d, %d, %d, %s ,%d ,%s, %d, %d, %d, %d, %d, %d, %s, %d, %d, %s, %d ",
						cfg.description,
						cfg.experimentStartTime,
						cfg.experimentLength,
						cfg.numberOfMessageHandlerThreads,
						cfg.numberOfMiddleWareMachines,
						cfg.numberOfDBConnections,
						cfg.numberOfClientMachines,
						cfg.numQueues,
						cfg.num_clients,
						readByAuthor.getRequestUUID(),
						this.clientId,
						"findAuthor",
						outTimes.clThinkTime,
						outTimes.clRoundTime,
						outTimes.mwThinkTime,
						outTimes.mwRoundTime,
						outTimes.dbRoundTime,
						outTimes.dbThinkTime,
						resp.success ? "[s]" : "[f]",							
						outTimes.mwTimeInDBQ,
						outTimes.clTimeInQ,
						"found",
						resp.success ? 1 : 0
						)
						+String.format(", %d, %d, %d, %d, %d, %d, %d, %d, %d",
								outTimes.clWaitsinMWQ-outTimes.clStarts, //Connection init time 
								outTimes.clClosedConn-outTimes.clRespFromMW, // Close connection time
								outTimes.mwGetsStreams-outTimes.mwStarts, //Open stream time on mw
								outTimes.mwSentReadyToClient-outTimes.mwGetsStreams, //Time to send ready Message
								outTimes.mwWaitsinDBQ-outTimes.mwSentReadyToClient, //Time to receive Request from client 
								outTimes.mwWaitsinDBQ-cfg.experimentStartTime,  //used for q size calc
								outTimes.mwOutofDBQ-cfg.experimentStartTime, //used for q size calc
								outTimes.clWaitsinMWQ-cfg.experimentStartTime,   //used for q size calc
								outTimes.clOutofMWQ-cfg.experimentStartTime  //used for q size calc
						)+String.format(", %d, %d, %d, %d, %d, %d",
								outTimes.qSizeDB,
								outTimes.qSizeMW,
								outTimes.mwNoQRound,
								outTimes.mwNetworkTime,
								outTimes.mwID,
								outTimes.mwReadyNetTime
						)
				);
		
	
		return message;
	}
	
	

	
	@Override
	public boolean deleteMessage(DeleteMessageRequest deleteMessageRequest) {
		
		return false;
	}
	
	
	
	@Deprecated
	public boolean closeConnection(){
		CloseConnection close = new CloseConnection();
		try {
			outputStream.writeObject(close);
			outputStream.close();
			inputStream.close();
			socket.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

}
