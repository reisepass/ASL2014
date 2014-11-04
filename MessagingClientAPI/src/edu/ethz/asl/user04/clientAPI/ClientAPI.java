package edu.ethz.asl.user04.clientAPI;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

@Deprecated
public class ClientAPI implements ClientAPIInterface {
	
	public final static Logger LOGGER = MessagingSystemLogger.getLoggerForClass(ClientAPI.class.getName());
	
	private Socket socket;
	private ObjectOutputStream outputStream;
	private ObjectInputStream  intputStream;
	int tooManyMessages=10000;
	int clientId;
	int NUMBER_OF_RR_POLLS = 5; // Number of times to poll for Response in Req-Resp communication
	boolean isRequestResponse = false; // Client is One-way by default
	int context = 0; // Default context is 0 (= Broadcast messages)
	public boolean debugModeOn=false;
	private String brokerServiceURL;
	
	public ClientAPI( String ip, int port) throws UnknownHostException, IOException{
		socket = new Socket(ip, port);
		
		outputStream = new ObjectOutputStream(socket.getOutputStream());
		intputStream = new ObjectInputStream( socket.getInputStream()); //TODO why new object ??

	}
	
	/**
	 * Returns an instance of ClientAPI for client to perform functions
	 * DO NOT USE THIS SINCE THIS DOES NOT MANDATE A CLIENT-ID
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	@Deprecated
	public ClientAPI() throws UnknownHostException, IOException{
		socket = new Socket("127.0.0.1", 5009);
		outputStream = new ObjectOutputStream(socket.getOutputStream());
		intputStream = new ObjectInputStream( socket.getInputStream()); //TODO why new object ??

	}
	
	/**
	 * Returns an instance of ClientAPI for client to perform functions
	 * MessageBrokerService URL defaults to 127.0.0.1
	 * @param clientID Client ID
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public ClientAPI(int clientID) throws UnknownHostException, IOException {
		this(clientID, "127.0.0.1");
	}
	
	/**
	 * Returns an instance of ClientAPI for client to perform functions
	 * @param clientID Client ID
	 * @param brokerServiceURL URL/DNS for the MessageBrokerService. Default is 127.0.0.1
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public ClientAPI(int clientID, String brokerServiceURL) throws UnknownHostException, IOException {
		this.clientId = clientID;
		this.brokerServiceURL = brokerServiceURL;
		
		LOGGER.info(String.format("[DEBUG] Client %d connecting to %s", clientID, brokerServiceURL));
		
		socket = new Socket(brokerServiceURL, 5009);
		outputStream = new ObjectOutputStream(socket.getOutputStream());
		intputStream = new ObjectInputStream( socket.getInputStream());
	}
	
	
	
	@Override
	public void setCliendID(int clientId) {
		this.clientId = clientId;
	}

	@Override
	public void enableRequestResponseMode() {
		this.isRequestResponse = true;
		
	}

	@Override
	public void disableRequestResponseMode() {
		this.isRequestResponse = false;
	
	}

	public boolean sendMessage(Message msg){
		return sendMessage(new WriteMessageRequest(msg, false));
	}
	
	@Override
	public boolean sendMessage(WriteMessageRequest writeMessageRequest) {
		writeMessageRequest.message.setSenderId(this.clientId);
		try{
			LOGGER.info(String.format(
					"type=%s, request_type=%s, request_id=%s, tag=%s", "request",
					"write_message_request",
					writeMessageRequest.getRequestUUID(), "send_request_from_api_to_server"));
			outputStream.writeObject(writeMessageRequest);
			//objectStream.close();
			if(debugModeOn)
				System.out.println("ClientAPI Waiting for confirmation in sendMessage");
			RequestResponse resp =(RequestResponse) intputStream.readObject();
			LOGGER.info(String.format(
					"type=%s, request_type=%s, request_id=%s, tag=%s", "response",
					"write_message_request",
					writeMessageRequest.getRequestUUID(), "receive_response_from_server_to_api"));
			if(debugModeOn)
				System.out.println("SendMessage Confirm Response with "+resp);
			return resp.success;
		}
		catch (Exception e){
			LOGGER.log(Level.WARNING, "SendMessage failed for RequestID" + writeMessageRequest.getRequestUUID(), e);
			return false;
		}
	}


	@Override
	public Message sendRequestResponseMessage(WriteMessageRequest writeMessageRequest, long timeout) {
		boolean flag = false;
		
		writeMessageRequest.message.setContext(this.context);
		writeMessageRequest.message.setSenderId(this.clientId);
		// See who the message is being sent to, so that we can query for
		// replies from client with the appropriate context
		int receiverId = writeMessageRequest.message.getReceiverId();
		
		// 1. Send the request message
		flag = sendMessage(writeMessageRequest);
		System.out.println("Dispatched Request-Response message: " + writeMessageRequest);
		
		// 2. Wait for Response message
		if (flag) {
			for (int i = 0; i < NUMBER_OF_RR_POLLS; i++) {
				System.out.println("Reading. Trying : " + i);
				System.out
						.println(String
								.format("Attemping to read  messages from client %d, in queue %d, with context %d",
										writeMessageRequest.message
												.getReceiverId(),
										writeMessageRequest.message
												.getQueueIdList().get(0),
										writeMessageRequest.message
												.getContext()));
				Message reply = readOnePrivateMessage(new ReadPrivateMessageRequest(
						receiverId, writeMessageRequest.message.getQueueIdList().get(0), true, true,
						this.context));
				if (reply == null) {
					try {
						Thread.sleep(timeout / NUMBER_OF_RR_POLLS); 
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				else {
					System.out.println("Reply received: " + reply);
					return reply;
				}
			}
		}
		return null;
	}

	/*@Override
	public List<Message> readAllPrivateMessage(int receiverID, int queueID,boolean removeAfterPeek,boolean  orderByTime) {
		List<Message> resultList = new ArrayList<Message>();
		try {
			int i=0;
			while(true){
				ReadAllPrivateMessagesRequest readAllRequest = null;
				readAllRequest = new ReadAllPrivateMessagesRequest(receiverID, queueID, removeAfterPeek, orderByTime,i);
				outputStream.writeObject(readAllRequest);
				try {
					//TODO add check case of null and non list format 
					Object obj =  intputStream.readObject();
					if(obj==null){
						System.err.println("<Error, readALLprivateMEssages returned a null");
					}
						
					 List<Message> result =(List<Message>)obj;
					if(result.size()==0)
						return resultList;
					resultList.addAll(result);
					i++;
					
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return resultList;
	}*/
	

	public List<Message>readMultipleMessages(int queueid,int senderID,boolean removeAfteReading, boolean orderByTime){
		
		int page=0;
		List<Message> totalList= new LinkedList<Message>();
		List<Message> result= new LinkedList<Message>();
		do {
			ReadAllMessagesRequest nextChunck=new ReadAllMessagesRequest(queueid,senderID,removeAfteReading,orderByTime,page);
			result=readMultipleMessages(nextChunck);
			totalList.addAll(result);
			page++;
		}while(result.size()>0);
		
		return totalList;
	}
	
	@Override
	public List<Message> readMultipleMessages(
			ReadAllMessagesRequest readAllRequest) {
		List<Message> resultList = new ArrayList<Message>();
		try {
			readAllRequest.setReceiverID(this.clientId);
			outputStream.writeObject(readAllRequest);
			try {
				// TODO add check case of null and non list format
				Object obj = intputStream.readObject();
				if (obj == null) {
					System.err.println("<Error, readALLprivateMEssages returned a null");
					return resultList;
				}
				if(obj instanceof RequestResponse){
					RequestResponse ooobj= (RequestResponse)obj;
					if( ooobj.success){
						List<Message> result = (List<Message>) ooobj.getPayload();
						return result;
					}
					else{
						return new LinkedList<Message>();//THis is bad it should throw excepion or something
					}
						
						
				}

				List<Message> result = (List<Message>) obj;
				return result;
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return resultList;
	}
	
	public Message readOnePrivateMessage(int clientID,int queueID,boolean removeAfterPeek,boolean orderByTime){
		ReadPrivateMessageRequest rpmr = new ReadPrivateMessageRequest(clientID, queueID, removeAfterPeek, orderByTime);
		return readOnePrivateMessage(rpmr);
	}
	
	@Override
	public Message readOnePrivateMessage(ReadPrivateMessageRequest readMessageRequest) {
		Message back = null;
		readMessageRequest.setReceiverID(this.clientId);
		
		try {
			outputStream.writeObject(readMessageRequest);
			 try {
				back= (Message)intputStream.readObject();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return back;
	}
	

	public Message readFromQueue ( ReadQueueRequest rqr){
		Message back = null;
		try {
			outputStream.writeObject(rqr);
			 try {
				back= (Message) intputStream.readObject();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return back;
		
	}

	public boolean deleteMessage(long messageID){
	
	return deleteMessage(new DeleteMessageRequest(this.clientId , messageID));
	}
	@Override
	public boolean deleteMessage(DeleteMessageRequest deleteMessageRequest) {
		try {
			outputStream.writeObject(deleteMessageRequest);			
			RequestResponse retVal = (RequestResponse) intputStream.readObject();
			return retVal.success;
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("couldnot create Queue");
			System.err.println(e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	
	
	public boolean createQueue(int queID,int clientId){
		CreateQueueRequest cqPack = new CreateQueueRequest(queID, clientId,"NoName");
		return createQueue(cqPack);
	}
	
	@Override
	public boolean createQueue(CreateQueueRequest createQueueRequest) {
		try {
			outputStream.writeObject(createQueueRequest);			
			RequestResponse retVal = (RequestResponse) intputStream.readObject();
			return retVal.success;
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("couldnot create Queue");
			e.printStackTrace();
		}
		
		return false;
	}
	
	
	public boolean deleteQueue(int queueId){
		return deleteQueue(new DeleteQueueRequest(queueId));
	}
	@Override
	public boolean deleteQueue(DeleteQueueRequest deleteQueueRequest) {
		try {
			outputStream.writeObject(deleteQueueRequest);			
			Boolean retVal = (Boolean) intputStream.readObject();
			//System.out.println("retVal: "+"middleware ret: "+retVal);
			return retVal;
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("couldnot create Queue");
			e.printStackTrace();
		}
		
		return false;
	}
	
	public ArrayList<Integer> getQueuesWithRelevantMessages(int ClientID){
		this.clientId=ClientID;
		return getQueuesWithRelevantMessages(new QueryForQueuesWithMessagesForMe(ClientID));
	}
	
	public ArrayList<Integer> getQueuesWithRelevantMessages(QueryForQueuesWithMessagesForMe queueRequest){
		queueRequest.setReceiverId(clientId);
		try{
			outputStream.writeObject(queueRequest);
			RequestResponse resp = (RequestResponse) intputStream.readObject();
			if(resp.getPayload() instanceof ArrayList<?> ){
				return (ArrayList<Integer>)resp.getPayload();
			}
			
		}
		catch (Exception e){
			System.out.println("couldont get list of queues");
			e.printStackTrace();
		}
		return null;
		
		
	}
	
	public boolean closeConnection(){
		CloseConnection close = new CloseConnection();
		try {
			outputStream.writeObject(close);
			outputStream.close();
			intputStream.close();
			socket.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	



	

}
