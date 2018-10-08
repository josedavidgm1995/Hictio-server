package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

public class Server extends Observable implements Runnable, Observer {

	private ServerSocket serverSocket;
	private SerialCom serialCom;
	private LinkedList<ClientAttention> clients_attentios;
	private static Server server = null;
	private boolean online = false;
	private int port;

	private Server(Observer observer, int port) {
		this.port = port;
		addObserver(observer);
		startServerSocket();
		this.clients_attentios = new LinkedList<ClientAttention>();
		this.online = true;

		this.serialCom = new SerialCom();
		new Thread(serialCom).start();
		this.serialCom.addObserver(this);
	}

	private void startServerSocket() {
		try {
			this.serverSocket = new ServerSocket(this.port);
			this.online = true;
			try {
				System.out.println("server_online at: " + InetAddress.getLocalHost());
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Server getInstance(Observer observer, int port) {
		if (server == null) {
			server = new Server(observer, port);
			new Thread(server).start();
		}

		return server;
	}

	@Override
	public void run() {
		while (this.online) {

			try {
				if (this.serverSocket != null) {
					// Blocking operation, waiting for a client
					Socket new_client_request = this.serverSocket.accept();
					// Create new client, with the socket attending to it
					ClientAttention new_attention = new ClientAttention(new_client_request, clients_attentios.size());
					// Add the Server as a Observer of that new_attention
					new_attention.addObserver(this);
					// Add the new_attention to the clients_attentios collection
					clients_attentios.add(new_attention);
					// Notify Logic about the new_attention
					setChanged();
					notifyObservers("add" + new_attention.getId());
					clearChanged();
					// start the thread of that new_attention
					new Thread(new_attention).start();
					System.out.println(
							"New client attention created - client attentions size:" + clients_attentios.size());

				} else {
					startServerSocket();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}
	}

	/** _______________________________________________________________ */

	public void sendPCDInteractions(String PCDClient,String fish, char c) {
		// TODO Auto-generated method stub
		for (ClientAttention clientAttention : clients_attentios) {
			if (clientAttention.getUID().contains(PCDClient)) {
				clientAttention.sendString(fish+"-"+c);
			} else {

			}
			
		}
	}

	/* __________________________________________________________ */
// Verify fish client has the fish with that int, and what part has been touch
// Depending on what part was touched, the server send the case.
	public void verifyFish(int fish, int part) {
		for (ClientAttention clientAttention : clients_attentios) {
			if (clientAttention.getFishId() == fish && clientAttention.isOnFish() == true) {
				switch (part) {
				case 0:

					clientAttention.sendString("head");
					break;
				case 1:

					clientAttention.sendString("middle");
					break;
				case 2:

					clientAttention.sendString("tail");
					break;

				}
				break;
			}
		}
	}

	// Verify into the collection of clients that nobody is already connected with
	// the same fish.
	// If nobody if connected with that fish, the server will asign that fish to the
	// client
	private void assignFish(ClientAttention client, String value) {

		int fishId = Integer.parseInt(value.split("_")[value.split("_").length - 1]);

		for (ClientAttention clientAttention : clients_attentios) {
			if (clientAttention.isOnFish() == true && clientAttention.getFishId() == fishId) {
				client.sendString("Sorry, you have to wait");
			} else {
				client.setFishId(fishId);
				client.setOnFish(true);
				client.sendString("onfish_" + client.getFishId());
				System.out.println("User: " + client.getId() + " conected with fish: " + client.getFishId());
			}
		}

	}

	// Send a fake beacon signal to the clients - just for testing

	public void sendFakeBeacon(char key) {
		// TODO Auto-generated method stub
		switch (key) {
		case '0':
			for (ClientAttention clientAttention : clients_attentios) {
				clientAttention.sendString("beacon_oscar");
				System.out.println("Send fake " + key + " beacon");
			}
			break;
		case '1':
			for (ClientAttention clientAttention : clients_attentios) {
				clientAttention.sendString("beacon_piranha");
				System.out.println("Send fake " + key + " beacon");
			}
			break;

		}

	}
	/* __________________________________________________________ */

	@Override
	public void update(Observable o, Object obj) {

		if (obj instanceof String) {
			String msn = ((String) obj);
			if (msn.contains("offClient")) {
				ClientAttention cli_atte = (ClientAttention) o;
//				setChanged();
//				notifyObservers("remove:" + cli_atte.getId());
//				clearChanged();
				clients_attentios.remove(cli_atte);
				System.out.println("Client attentions size: " + this.clients_attentios.size());
			} else if (msn.contains("fish")) {

				this.assignFish((ClientAttention) o, (String) obj);

			} else if (msn.contains("haptic")) {
				setChanged();
				notifyObservers((String) obj);
				clearChanged();
			} else if (msn.contains("PC")) {
				setChanged();
				notifyObservers((String) obj);
				clearChanged();
			}

		}

	}

	public void closeServer() {

		try {
			for (ClientAttention clientAttention : clients_attentios) {
				clientAttention.sendString("x");
				clientAttention.getSocket_atention().close();
			}
			this.online = false;

			System.err.println("Server die");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
