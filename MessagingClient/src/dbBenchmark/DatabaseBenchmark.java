package dbBenchmark;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.ethz.asl.user04.Clients2014.ClientSimulator;
import edu.ethz.asl.user04.Clients2014.StdClient;
import edu.ethz.asl.user04.Clients2014.StdPopClient;
import edu.ethz.asl.user04.Clients2014.StdSendClient;
import edu.ethz.asl.user04.clientAPI.MessageAPI2014;
import edu.ethz.asl.user04.dbutils.DBManager;
import edu.ethz.asl.user04.shared.entity.ConfigExperimentV2014;
import edu.ethz.user04.shared.requests.queuerequests.CreateQueueRequest;

public class DatabaseBenchmark {

	private ExecutorService pool;
	Properties exp_prop;
	DBManager dbPool;
	public DatabaseBenchmark(Properties exp_prop){
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

		int num_doNothing = 0;
		try{
		num_doNothing = Integer.parseInt(exp_prop
				.getProperty("num_donothing"));
		}
		catch(Exception e){
			System.err.println(" Could not find num_doNothing attribute");
		}
		long bufferLength=700;
		try{
		bufferLength = Integer.parseInt(exp_prop
				.getProperty("send_bufferlength"));
		}
		catch(Exception e){
			System.err.println(" Could not find send_bufferLength attribute");
		}
		
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
				+ num_pullPrivate + num_findAuthor + num_relQueue + num_doNothing;

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
		// ConfigExperimentV2014 expCfg = new ConfigExperimentV2014();
		
		
	

		
		dbPool= new DBManager(exp_prop.getProperty("db_username"),
				exp_prop.getProperty("db_url"),
				exp_prop.getProperty("db_port"),
				exp_prop.getProperty("db_name"),
				exp_prop.getProperty("db_password"),
				Integer.parseInt(exp_prop.getProperty("db_connection_limit")));
		

		ArrayList<Runnable> list = new ArrayList<Runnable>();
		int idCounter = start_idx_request_response;

		ArrayList<Integer> cliReqPrivMessage = new ArrayList<Integer>();
		ArrayList<Integer> authorList = new ArrayList<Integer>();
		
		
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
		
		
		
		for (int i = 0; i < num_senders; i++) {
			int nextCliID = idCounter++;
			authorList.add(nextCliID);
			list.add(new dbSender(nextCliID,message_length,dbPool,bufferLength,expCfg));

		}
		
		
		for (int i = 0; i < num_pullClients; i++) {
			int nextCliID = idCounter++;
			MessageAPI2014 capi;
			list.add(new dbReader(nextCliID,dbPool,bufferLength,expCfg));

		}
		
		if(debugOn)
			System.out.println("## Executor starting");
		pool = Executors.newFixedThreadPool(num_senders+num_pullClients + 3);
		for (Runnable cli : list) {
			pool.execute(cli);
		}
		if(debugOn)
			System.out.println("Done Executing Pool");
	}
	
	public static void main(String[] args) {
		Properties exp_prop = new Properties();
		try {
			exp_prop.load(new FileInputStream("properties/client.properties"));
			DatabaseBenchmark cliSim = new DatabaseBenchmark(exp_prop);
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
