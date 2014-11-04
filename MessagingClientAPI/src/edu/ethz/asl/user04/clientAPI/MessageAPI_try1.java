package edu.ethz.asl.user04.clientAPI;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import edu.ethz.asl.user04.shared.entity.ConfigExperimentV2;
import edu.ethz.asl.user04.shared.entity.Message;
import edu.ethz.asl.user04.shared.entity.RequestResponse;
import edu.ethz.asl.user04.shared.logging.MessagingSystemLogger;
import edu.ethz.user04.shared.requests.messagerequests.DeleteMessageRequest;
import edu.ethz.user04.shared.requests.messagerequests.WriteMessageRequest;
import edu.ethz.user04.shared.requests.queuerequests.CloseConnection;
import edu.ethz.user04.shared.requests.queuerequests.CreateQueueRequest;
import edu.ethz.user04.shared.requests.queuerequests.DeleteQueueRequest;
import edu.ethz.user04.shared.requests.queuerequests.QueryForQueuesWithMessagesForMe;
import edu.ethz.user04.shared.requests.queuerequests.ReadAllMessagesRequest;
import edu.ethz.user04.shared.requests.queuerequests.ReadPrivateMessageRequest;
import edu.ethz.user04.shared.requests.queuerequests.ReadQueueRequest;


public class MessageAPI_try1 extends MessageAPI implements ClientAPIInterface {
	
	public final static Logger LOGGER = MessagingSystemLogger.getLoggerForClass(MessageAPI_try1.class.getName());
	
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
	public ConfigExperimentV2 cfg;
	
	// OT - If this is enabled, the message handler keeps listening for objects.
	// Message Handler terminates only when it receives a CloseConnectionObject
	// This should match BATCH_REQUESTS in MessageHandler
	// Maybe read this from config?
	private final static boolean BATCH_REQUESTS = false;
	
	String brokerServiceURL;
	int port;
	
	public MessageAPI_try1( String brokerServiceURL, int port) throws UnknownHostException, IOException{
		this.brokerServiceURL=brokerServiceURL;
		this.port=port;
		cfg=null;
	}
	
	/**
	 * Returns an instance of ClientAPI for client to perform functions
	 * MessageBrokerService URL defaults to 127.0.0.1
	 * MessageBrokerService Port defaults to 5009
	 * @param clientID Client ID
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public MessageAPI_try1(int clientID) throws UnknownHostException, IOException {
		this(clientID, DEFAULT_SERVER_URL);
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
	public MessageAPI_try1(int clientID, String brokerServiceURL) throws UnknownHostException, IOException {
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
	public MessageAPI_try1(int clientID, String brokerServiceURL, int port) throws UnknownHostException, IOException {
		this.clientId = clientID;
		this.brokerServiceURL = brokerServiceURL;
		this.port = port;
		
		LOGGER.info(String.format("[DEBUG] Client %d connecting to %s", clientID, brokerServiceURL));
		cfg=null;
		// socket = new Socket(brokerServiceURL, 5009);
		// outputStream = new ObjectOutputStream(socket.getOutputStream());
		// inputStream = new ObjectInputStream( socket.getInputStream());
	}
	
	public MessageAPI_try1(int clientID, String brokerServiceURL, int port, ConfigExperimentV2 cfg) throws UnknownHostException, IOException {
		this.clientId = clientID;
		this.brokerServiceURL = brokerServiceURL;
		this.port = port;
		
		LOGGER.info(String.format("[DEBUG] Client %d connecting to %s", clientID, brokerServiceURL));
		this.cfg = cfg;
	}
	
	
	@Deprecated
	/**
	 * DO NOT use this anymore. Make MessageAPI mandate ClientID to be passed.
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public MessageAPI_try1() throws UnknownHostException, IOException{
	this.brokerServiceURL="127.0.0.1";
	this.port=5009;

	}
	
	
	private void initConnection(){//TODO force retry after certan sleep period
		try {
			socket = new Socket(brokerServiceURL, port);
			outputStream = new ObjectOutputStream(socket.getOutputStream());
			inputStream = new ObjectInputStream( socket.getInputStream()); //TODO why new object ??
		} catch (UnknownHostException e) {
			LOGGER.log(Level.SEVERE,"MessageAPI unable to connect to the Middleware",e);
			e.printStackTrace();
			
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE,"MessageAPI unable to connect to the Middleware",e);
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
		Socket tempSocket = null;
		ObjectOutputStream outputStream = null;
		ObjectInputStream inputStream = null;
		
		RequestResponse resp = new RequestResponse(false);
		writeMessageRequest.message.setSenderId(this.clientId);

		try {
			tempSocket = new Socket(brokerServiceURL, port);

			// Write Request object
			outputStream = new ObjectOutputStream(tempSocket.getOutputStream());
			inputStream = new ObjectInputStream(tempSocket.getInputStream());
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"request", "WriteMessageRequest",
					writeMessageRequest.getRequestUUID(),
					"sending_request_from_api_to_server", this.clientId));
			outputStream.writeObject(writeMessageRequest);
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"request", "WriteMessageRequest",
					writeMessageRequest.getRequestUUID(),
					"sent_request_from_api_to_server", this.clientId));

			// Read Response object
			
			resp = (RequestResponse) inputStream.readObject();
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"response", "WriteMessageRequest",
					writeMessageRequest.getRequestUUID(),
					"received_response_at_api_from_server", this.clientId));
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "SendMessage failed for RequestID "
					+ writeMessageRequest.getRequestUUID(), e);
		}
		
		try {
			outputStream.close();
			inputStream.close();
			tempSocket.close();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to close socket"
					+ writeMessageRequest.getRequestUUID(), e);
		}
		
		LOGGER.log(
				Level.INFO,
				String.format(
						"[METRIC] type=response request_type=WriteMessageRequest request_id=%s tier=client_api tag=return_response_from_api_to_client client_id=%d",
						writeMessageRequest.getRequestUUID(), this.clientId));

		return resp.success;
	}

	// context is the context in the message and not the context in this class
	@Override
	public Message sendRequestResponseMessage(
			WriteMessageRequest writeMessageRequest, long timeout) {
		
		boolean flag = false;

		try {
			int context = writeMessageRequest.message.getContext();
			//System.out.println("context: "+writeMessageRequest.message.getContext());
			//writeMessageRequest.message.setContext(this.context);
			writeMessageRequest.message.setSenderId(this.clientId);
			// See who the message is being sent to, so that we can query for
			// replies from client with the appropriate context
			int receiverId = writeMessageRequest.message.getReceiverId();
			
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"request", "RequestResponseMessage",
					writeMessageRequest.getRequestUUID(),
					"sending_request_from_api_to_server", this.clientId));
			
			// 1. Send the request message
			LOGGER.log(Level.INFO, String.format(
					"[DEBUG] Sending Request %s from client %d to client %d within context %d",
					writeMessageRequest.getRequestUUID(), this.clientId,
					receiverId, context));
			flag = sendMessage(writeMessageRequest);
			
			LOGGER.log(
					Level.INFO,
					String.format(
							"[DEBUG] Client %d waiting for response from Client %d within context %d",
							this.clientId, receiverId, context));
			
			// 2. Wait for Response message
			if (flag) {
				for (int i = 0; i < NUMBER_OF_RR_POLLS; i++) {
					LOGGER.info("[DEBUG] Reading. Trying : " + i);
					LOGGER.info(String
									.format("[DEBUG]Attemping to read  messages from client %d, in queue %d, within context %d",
											writeMessageRequest.message
													.getReceiverId(),
											writeMessageRequest.message
													.getQueueIdList().get(0),
											context));
					// Attempt reading from the other client, specifying the queue and context
					ReadPrivateMessageRequest readRequest = new ReadPrivateMessageRequest(
							receiverId, writeMessageRequest.message.getQueueIdList().get(0), true, true,
							context);
					//readRequest.dontGetBroadCast();  // so that i get responses intended for me
					// and not broadcast responses
					
					Message reply = readOnePrivateMessage(readRequest);
					
					if (reply == null) {
						try {
							Thread.sleep(timeout / NUMBER_OF_RR_POLLS); 
						} catch (InterruptedException e) {
							LOGGER.log(
									Level.WARNING,
									"Something went wrong while polling for response for request "
											+ writeMessageRequest
													.getRequestUUID(), e);
						}
					}
					else {
						LOGGER.info(String.format(
								"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
								"response", "RequestResponseMessage",
								writeMessageRequest.getRequestUUID(),
								"received_response_at_api_from_server", this.clientId));
						LOGGER.info(String.format(
								"[DEBUG] Received reply %s for request %s.",
								reply, writeMessageRequest.getRequestUUID()));
						return reply;
					}
				}
			}
			
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "SendMessage failed for RequestID"
					+ writeMessageRequest.getRequestUUID(), e);
		}
		
		// Something went wrong. Return null.
		return null;

	}

	
	@Override
	public List<Message> readMultipleMessages(
			ReadAllMessagesRequest readAllRequest) {
		Socket tempSocket = null;
		ObjectOutputStream outputStream = null;
		ObjectInputStream inputStream = null;
		
		List<Message> resultList = new ArrayList<Message>();
		RequestResponse respObj = new RequestResponse(false);
		
		readAllRequest.setReceiverID(this.clientId);

		try {
			tempSocket = new Socket(brokerServiceURL, port);

			// Write Request object
			outputStream = new ObjectOutputStream(tempSocket.getOutputStream());
			inputStream = new ObjectInputStream(tempSocket.getInputStream());
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"request", "ReadAllMessagesRequest",
					readAllRequest.getRequestUUID(),
					"sending_request_from_api_to_server", this.clientId));
			
			outputStream.writeObject(readAllRequest);
			
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"request", "ReadAllMessagesRequest",
					readAllRequest.getRequestUUID(),
					"sent_request_from_api_to_server", this.clientId));

			// Read Response object
			
			Object obj = inputStream.readObject();
			
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"response", "ReadAllMessagesRequest",
					readAllRequest.getRequestUUID(),
					"received_response_at_api_from_server", this.clientId));	
			
			if (obj == null) {
				LOGGER.log(Level.WARNING, String.format(
						"[DEBUG] ReadAllMessagesRequest %s returned null",
						readAllRequest.getRequestUUID()));
			} else
				resultList = (List<Message>) respObj.getPayload();

		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "SendMessage failed for RequestID"
					+ readAllRequest.getRequestUUID(), e);
		}
		
		try {
			outputStream.close();
			inputStream.close();
			tempSocket.close();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to close socket "
					+ readAllRequest.getRequestUUID(), e);
		}

		return resultList;
	}
	

	
	@Override
	public Message readOnePrivateMessage(ReadPrivateMessageRequest readMessageRequest) {
		Socket tempSocket = null;
		ObjectOutputStream outputStream = null;
		ObjectInputStream inputStream = null;
		
		Message message = null;
		
		readMessageRequest.setReceiverID(this.clientId);
		
		try {
			tempSocket = new Socket(brokerServiceURL, port);

			// Write Request object
			outputStream = new ObjectOutputStream(tempSocket.getOutputStream());
			inputStream = new ObjectInputStream(tempSocket.getInputStream());
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"request", "ReadPrivateMessageRequest",
					readMessageRequest.getRequestUUID(),
					"sending_request_from_api_to_server", this.clientId));
			outputStream.writeObject(readMessageRequest);
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"request", "ReadPrivateMessageRequest",
					readMessageRequest.getRequestUUID(),
					"sent_request_from_api_to_server", this.clientId));

			// Read Response object
			
			message = (Message) inputStream.readObject();
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"response", "ReadPrivateMessageRequest",
					readMessageRequest.getRequestUUID(),
					"received_response_at_api_from_server", this.clientId));

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
		
		
		LOGGER.log(
				Level.INFO,
				String.format(
						"[METRIC] type=response request_type=ReadPrivateMessageRequest request_id=%s tier=client_api tag=return_response_from_api_to_client",
						readMessageRequest.getRequestUUID()));

		return message;
	}
	
	


	
	@Override
	public boolean deleteMessage(DeleteMessageRequest deleteMessageRequest) {
		RequestResponse response = new RequestResponse(false);
		Socket tempSocket = null;
		ObjectOutputStream outputStream = null;
		ObjectInputStream inputStream = null;
		
		deleteMessageRequest.setClientid(this.clientId);
		
		try {
			tempSocket = new Socket(brokerServiceURL, port);

			// Write Request object
			outputStream = new ObjectOutputStream(tempSocket.getOutputStream());
			inputStream = new ObjectInputStream(tempSocket.getInputStream());
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"request", "DeleteMessageRequest",
					deleteMessageRequest.getRequestUUID(),
					"sending_request_from_api_to_server", this.clientId));
			
			outputStream.writeObject(deleteMessageRequest);
			
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"request", "DeleteMessageRequest",
					deleteMessageRequest.getRequestUUID(),
					"sent_request_from_api_to_server", this.clientId));

			// Read Response object
			
			response = (RequestResponse) inputStream.readObject();

			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"response", "DeleteMessageRequest",
					deleteMessageRequest.getRequestUUID(),
					"received_response_at_api_from_server", this.clientId));

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "SendMessage failed for RequestID"
					+ deleteMessageRequest.getRequestUUID(), e);
		}
		
		try {
			outputStream.close();
			inputStream.close();
			tempSocket.close();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to close socket "
					+ deleteMessageRequest.getRequestUUID(), e);
		}

		return response.success;
	}
	
	
	@Override
	public boolean createQueue(CreateQueueRequest createQueueRequest) {
		RequestResponse response = new RequestResponse(false);
		Socket tempSocket = null;
		ObjectOutputStream outputStream = null;
		ObjectInputStream inputStream = null;
		
		createQueueRequest.setClientid(this.clientId);
		
		try {
			tempSocket = new Socket(brokerServiceURL, port);

			// Write Request object
			outputStream = new ObjectOutputStream(tempSocket.getOutputStream());
			inputStream = new ObjectInputStream(tempSocket.getInputStream());
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"request", "CreateQueueRequest",
					createQueueRequest.getRequestUUID(),
					"sending_request_from_api_to_server", this.clientId));
			
			outputStream.writeObject(createQueueRequest);
			
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"request", "CreateQueueRequest",
					createQueueRequest.getRequestUUID(),
					"sent_request_from_api_to_server", this.clientId));
			
			// Read Response object
			
			response = (RequestResponse) inputStream.readObject();
			
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"response", "CreateQueueRequest",
					createQueueRequest.getRequestUUID(),
					"received_response_at_api_from_server", this.clientId));

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
		
		LOGGER.log(
				Level.INFO,
				String.format(
						"[METRIC] type=response request_type=CreateQueueRequest request_id=%s tier=client_api tag=return_response_from_api_to_client",
						createQueueRequest.getRequestUUID()));

		return response.success;
	}

	
	@Override
	public boolean deleteQueue(DeleteQueueRequest deleteQueueRequest) {
		Boolean response = false;
		Socket tempSocket = null;
		ObjectOutputStream outputStream = null;
		ObjectInputStream inputStream = null;
		
		try {
			tempSocket = new Socket(brokerServiceURL, port);

			// Write Request object
			outputStream = new ObjectOutputStream(tempSocket.getOutputStream());
			inputStream = new ObjectInputStream(tempSocket.getInputStream());
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"request", "DeleteQueueRequest",
					deleteQueueRequest.getRequestUUID(),
					"sending_request_from_api_to_server", this.clientId));
			
			outputStream.writeObject(deleteQueueRequest);
			
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"request", "DeleteQueueRequest",
					deleteQueueRequest.getRequestUUID(),
					"sent_request_from_api_to_server", this.clientId));

			// Read Response object
			
			response = (Boolean) inputStream.readObject();

			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"response", "DeleteQueueRequest",
					deleteQueueRequest.getRequestUUID(),
					"received_response_at_api_from_server", this.clientId));

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

		return response;
	}
	
	
	public ArrayList<Integer> getQueuesWithRelevantMessages(QueryForQueuesWithMessagesForMe queueRequest){
		// This method returns null in case something goes wrong.
		ArrayList<Integer> queuesWithRelevantMessages = null;
		RequestResponse response = new RequestResponse(false);
		Socket tempSocket = null;
		ObjectOutputStream outputStream = null;
		ObjectInputStream inputStream = null;
		
		queueRequest.setReceiverId(this.clientId);
		
		try {
			tempSocket = new Socket(brokerServiceURL, port);

			// Write Request object
			outputStream = new ObjectOutputStream(tempSocket.getOutputStream());
			inputStream = new ObjectInputStream(tempSocket.getInputStream());
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"request", "QueryForQueuesWithMessagesForMe",
					queueRequest.getRequestUUID(),
					"sending_request_from_api_to_server", this.clientId));
			outputStream.writeObject(queueRequest);
			
			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"request", "QueryForQueuesWithMessagesForMe",
					queueRequest.getRequestUUID(),
					"sending_request_from_api_to_server", this.clientId));

			// Read Response object
			
			response = (RequestResponse) inputStream.readObject();

			LOGGER.info(String.format(
					"[METRIC] type=%s request_type=%s request_id=%s tag=%s client_id=%d",
					"response", "QueryForQueuesWithMessagesForMe",
					queueRequest.getRequestUUID(),
					"received_response_at_api_from_server", this.clientId));
			
			if (response.success) {
				if (response.getPayload() instanceof ArrayList<?>) {
					queuesWithRelevantMessages = (ArrayList<Integer>) response
							.getPayload();
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

		return queuesWithRelevantMessages;	
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
