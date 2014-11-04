package edu.ethz.asl.user04.messagebroker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Deprecated
public class MessageServer {
	
	/**
	 * DO NOT USE THIS!
	 *   -- OT
	 */
	
	private static final int HANDLERS = 10;
	private static final int SERVER_PORT = 5007;
	
	ServerSocket serverSocket;
	ExecutorService pool;
	
	/**
	 * ListenerService
	 * @author Tribhuvanesh
	 *
	 */
	class ListenerService implements Runnable {
		private ServerSocket serverSocket = null;
	    public ListenerService() throws IOException {
	        serverSocket = new ServerSocket(SERVER_PORT);
	    }
	     
	    @Override
	    public void run() {
	        try {
	            System.out.println("ListenerService: waiting for new clients");
	            while ( true ) {
	                // Block and wait for a client connection
	                Socket client = serverSocket.accept();
	                 
	                // Assign a thread to handle client network communication
	                System.out.println("ListenerService: client connected...");
	                ClientHandler handler = new ClientHandler(client);
	                pool.execute(handler);
	            }
	        }
	        catch ( Exception e ) {
	            e.printStackTrace();
	            pool.shutdown();
	        }
	    }
	}
	
	/**
	 * Client Handler
	 * @author Tribhuvanesh
	 *
	 */
	class ClientHandler implements Runnable {
		
		private final Socket socket;
		
		public ClientHandler(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			try {
				ObjectInputStream oi = new ObjectInputStream(socket.getInputStream());
				String message = (String) oi.readObject();
				System.out.println("Reading this message: "+ message.toLowerCase());
				
				oi.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public MessageServer() {
		// serverSocket = new ServerSocket(SERVER_PORT);
		ListenerService listener;
		try {
			pool = Executors.newFixedThreadPool(HANDLERS);
			listener = new ListenerService();
	        pool.submit(listener);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void shutdown() {
		pool.shutdown();
	}

	public static void main(String[] args) throws IOException {
		
		MessageServer messageServer = new MessageServer();
		
		String[] messages = {"Velociraptor", "T-rex", "Brontosaurus", "Stegosaurus"};
		
		// Send a message
		Socket socket = null;
		try {
			for (String message : messages) {
				socket = new Socket("127.0.0.1", 5007);
				
				ObjectOutputStream oo = new ObjectOutputStream(socket.getOutputStream());
				
				oo.writeObject(message);
				oo.close();
				socket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			socket.close();
		}
		
		messageServer.shutdown();
	}


}
