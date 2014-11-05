package edu.ethz.asl.user04.Clients2014;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.ethz.asl.user04.clientAPI.MessageAPI2014;
import edu.ethz.asl.user04.shared.entity.ConfigExperimentV2014;
import edu.ethz.user04.shared.requests.queuerequests.CreateQueueRequest;
import edu.ethz.user04.shared.requests.queuerequests.DeleteQueueRequest;

public class ClientSimulator {
	private ExecutorService pool;
	Properties exp_prop;
	
	// Constructor Should take some config file about which clients we want
	public ClientSimulator(Properties exp_prop) {
		
		this.exp_prop = exp_prop;
		
	}
	public void doStuff(){
		
		int num_senders = Integer.parseInt(exp_prop
				.getProperty("num_send_only"));
		int num_privateSenders = Integer.parseInt(exp_prop
				.getProperty("num_send_private"));
		int num_pullClients = Integer.parseInt(exp_prop
				.getProperty("num_pullclients"));
		int num_findAuthor = Integer.parseInt(exp_prop
				.getProperty("num_findauthor"));
		int num_relQueue = Integer.parseInt(exp_prop
				.getProperty("num_relqueue"));
		int num_pullPrivate = Integer.parseInt(exp_prop
				.getProperty("num_pullprivate"));
		int num_peekclients = Integer.parseInt(exp_prop
				.getProperty("num_peekclients"));

		boolean debugOn = Boolean.parseBoolean(exp_prop
				.getProperty("debug_on"));


		int num_queues = Integer.parseInt(exp_prop.getProperty("num_queues"));
		int message_length = Integer.parseInt(exp_prop
				.getProperty("message_length"));
		int start_idx_request_response = Integer.parseInt(exp_prop
				.getProperty("start_idx_request_response"));

		String middlewareIP = exp_prop.getProperty("mw_url");
		int middlewarePort = Integer.parseInt(exp_prop.getProperty("mw_port"));
		int totalThreadsNeeded = num_senders + num_peekclients
				+ num_privateSenders + num_pullClients + num_pullClients
				+ num_pullPrivate + num_findAuthor + num_relQueue;

		ConfigExperimentV2014 expCfg = new ConfigExperimentV2014(
				Integer.parseInt(exp_prop
						.getProperty("mw_message_handlers_pool_size")),
				Integer.parseInt(exp_prop.getProperty("db_connection_limit")),
				Integer.parseInt(exp_prop.getProperty("num_middlewares")),
				Integer.parseInt(exp_prop.getProperty("num_client_machines")),
				num_queues,
				Integer.parseInt(exp_prop.getProperty("dbFillLevelPerQueue")),
				message_length,
				Integer.parseInt(exp_prop.getProperty("experiment_duration")),
				exp_prop.getProperty("description"),
				Integer.parseInt(exp_prop.getProperty("num_clients_permachine")));

		long waitBeforeDeleteQueue= Long.parseLong(exp_prop.getProperty("experiment_duration"))-5000;
		try{
		waitBeforeDeleteQueue = Long.parseLong(exp_prop
				.getProperty("whenDeleteQueue"));
		}
		catch(Exception e){
			System.err.println(" Could not find waitBeforeDeleteQueue attribute");
		}
		// ConfigExperimentV2014 expCfg = new ConfigExperimentV2014();

		
		
		

		if(debugOn)
			System.out.println("## Client.properties read in complete");
		
		// //////////// Setup DB env
		try {
			MessageAPI2014 setupMAPI = new MessageAPI2014(100, middlewareIP,
					middlewarePort, expCfg);
			if(debugOn)
				System.out.println(String.format("## Created this MAPI   %s:%d",middlewareIP,middlewarePort));
			
			if(debugOn)
				System.out.println("## MessageAPI instant complete");
			
			for (int i = 1; i <= num_queues; i++) {
				CreateQueueRequest cqr = new CreateQueueRequest(i, 100,
						"StdQueue" + i);
				if(debugOn)
					System.out.println("## Q created");
				setupMAPI.createQueue(cqr);
				if(debugOn)
					System.out.println("## Q sent");
			}
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		ArrayList<StdClient> list = new ArrayList<StdClient>();
		int idCounter = start_idx_request_response;

		ArrayList<Integer> cliReqPrivMessage = new ArrayList<Integer>();
		ArrayList<Integer> authorList = new ArrayList<Integer>();

		for (int i = 0; i < num_pullPrivate; i++) {
			int nextCliID = idCounter++;
			cliReqPrivMessage.add(nextCliID);
			MessageAPI2014 capi;
			try {
				capi = new MessageAPI2014(nextCliID, middlewareIP,
						middlewarePort, expCfg);
				list.add(new StdReadPrivate(nextCliID, capi, expCfg, -1, true,
						num_queues));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		for (int i = 0; i < num_relQueue; i++) {
			int nextCliID = idCounter++;
			cliReqPrivMessage.add(nextCliID);
			MessageAPI2014 capi;
			try {
				capi = new MessageAPI2014(nextCliID, middlewareIP,
						middlewarePort, expCfg);
				list.add(new StdFindRelQue(nextCliID, capi, expCfg));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		for (int i = 0; i < num_pullClients; i++) {
			int nextCliID = idCounter++;
			MessageAPI2014 capi;
			try {
				capi = new MessageAPI2014(nextCliID, middlewareIP,
						middlewarePort, expCfg);
				list.add(new StdPopClient(nextCliID, capi, expCfg,
						(int) (nextCliID % num_queues) + 1));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		for (int i = 0; i < num_peekclients; i++) {
			int nextCliID = idCounter++;
			MessageAPI2014 capi;
			try {
				capi = new MessageAPI2014(nextCliID, middlewareIP,
						middlewarePort, expCfg);
				list.add(new StdPeekClient(nextCliID, capi, expCfg,
						(int) (nextCliID % num_queues) + 1));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		

		for (int i = 0; i < num_senders; i++) {
			int nextCliID = idCounter++;
			MessageAPI2014 capi;
			try {
				capi = new MessageAPI2014(nextCliID, middlewareIP,
						middlewarePort, expCfg);
				authorList.add(nextCliID);
				list.add(new StdSendClient(nextCliID, message_length, capi,
						expCfg, (int) (Math.random() * 1000 % num_queues) + 1));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		for (int i = 0; i < num_findAuthor; i++) {
			if (authorList.size() < 1) {
				System.err
						.println("[ERROR]: Can not find authors if no authors are being simulated");
				throw new EmptyStackException();
			}
			int nextCliID = idCounter++;
			cliReqPrivMessage.add(nextCliID);
			int tmpQ = (int) (Math.random() * 1000 % num_queues) + 1;
			try {
				MessageAPI2014 capi = new MessageAPI2014(nextCliID,
						middlewareIP, middlewarePort, expCfg);
				list.add(new StdReadByAuthor(nextCliID, capi, expCfg, tmpQ,
						authorList, false));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		for (int i = 0; i < num_privateSenders; i++) {
			int nextCliID = idCounter++;
			MessageAPI2014 capi;
			try {
				capi = new MessageAPI2014(nextCliID, middlewareIP,
						middlewarePort, expCfg);
				list.add(new StdSendPrivate(nextCliID, message_length, capi,
						expCfg, (int) (Math.random() * 1000 % num_queues) + 1,
						cliReqPrivMessage));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		for( int i =1; i < num_queues ; i++){
			MessageAPI2014 capi;
			try {
				capi = new MessageAPI2014(100, middlewareIP,
						middlewarePort, expCfg);
				list.add(new StdDeletQueue(100, capi,expCfg,i,waitBeforeDeleteQueue));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(debugOn)
			System.out.println("## Executor starting");
		pool = Executors.newFixedThreadPool(totalThreadsNeeded + num_queues + 3);
		for (StdClient cli : list) {
			pool.execute(cli);
		}
		
		if(debugOn)
			System.out.println("Done Executing Pool");
		
		
		
		
	}
		

	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	static Random rnd = new Random();

	String randomString(int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++)
			sb.append(AB.charAt(rnd.nextInt(AB.length())));
		return sb.toString();
	}

	public static void main(String[] args) {
		Properties exp_prop = new Properties();
		try {
			exp_prop.load(new FileInputStream("properties/client.properties"));

			ClientSimulator cliSim = new ClientSimulator(exp_prop);
			cliSim.doStuff();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
