package secure.chat;

/**
 * Nakov Chat Server
 * (c) Svetlin Nakov, 2002
 *
 * ServerDispatcher class is purposed to listen for messages received
 * from clients and to dispatch them to all the clients connected to the
 * chat server.
 */
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.codahale.shamir.Scheme;

public class ServerDispatcher extends Thread {
	private final static String myCipherSuite = "ecdh-secp521r1+X.509+AES_128/GCM/NoPadding"; // for AES/GCM
	private final static String symCipher = "AES_128/GCM/NoPadding"; // symmetric cipher for AES/GCM
	// private final static String myCipherSuite =
	// "ecdh-secp224r1+X.509+AES/CBC/PKCS5Padding"; // for AES/CBC
	// private final static String symCipher="AES/CBC/PKCS5Padding"; // symmetric
	// cipher for AES/CBC
	private final static String keyEstAlgor = "ecdh"; // key establish algorithm
	private final static String keyEstSpec = "secp521r1"; // specific parameter for key establish algorithm
	private final static String integrity = "X.509"; // a means for ensuring integrity of public key
	private KeyExchange myKey;
	private KeyStore myKeyStore;
	private String myAlias;

	int numberOfClients = 2;
	int threshold = 2;

	// partition according to client number
	final Scheme scheme = new Scheme(new SecureRandom(), numberOfClients, threshold);

	Set<String> receivedSK = new HashSet<String>();
	Map<Integer, byte[]> partsSK;

	byte[] recoveredPrivateKey = null;

	private static Vector<String> mMessageQueue = new Vector<String>();
	private static HashMap<String, ClientInfo> mClients = new HashMap<String, ClientInfo>(); // Hashmap <alias,
																								// ClientInfo>

	/**
	 * Adds given client to the server's client list.
	 */
	public synchronized void addClient(ClientInfo aClientInfo) {
		mClients.put(aClientInfo.mAlias, aClientInfo);
	}

	/**
	 * Deletes given client from the server's client list
	 * 
	 * @throws IOException
	 */
	public synchronized void deleteClient(ClientInfo aClientInfo) {
		if (mClients.containsKey(aClientInfo.mAlias))
			mClients.remove(aClientInfo.mAlias);
	}

	/**
	 * Adds given message to the dispatcher's message queue and notifies this thread
	 * to wake up the message queue reader (getNextMessageFromQueue method).
	 * dispatchMessage method is called by other threads (ClientListener) when a
	 * message is arrived.
	 */
	public synchronized void dispatchMessage(ClientInfo aClientInfo, String aMessage) {
		// aMessage = aClientInfo.mAlias + "/" + aMessage;
		mMessageQueue.add(aMessage);
		notify();
	}

	/**
	 * @return and deletes the next message from the message queue. If there is no
	 *         messages in the queue, falls in sleep until notified by
	 *         dispatchMessage method.
	 */
	private synchronized String getNextMessageFromQueue() throws InterruptedException {
		while (mMessageQueue.size() == 0)
			wait();
		String message = (String) mMessageQueue.get(0);
		mMessageQueue.removeElementAt(0);
		return message;
	}

	/**
	 * Sends given message to all clients in the client list. Actually the message
	 * is added to the client sender thread's message queue and this client sender
	 * thread is notified.
	 */
	private synchronized void sendMessageToAllClients(String aMessage) {
		for (Map.Entry<String, ClientInfo> entry : mClients.entrySet()) {
			ClientInfo clientInfo = entry.getValue();
			clientInfo.mClientSender.sendMessage(aMessage);
		}
	}

	// 2nd mod
	private synchronized Map<Integer, byte[]> distributeKey(String privateKey) {

		final byte[] secret = privateKey.getBytes(StandardCharsets.UTF_8);
		final Map<Integer, byte[]> parts = scheme.split(secret);

		int clients = 1;
		for (Map.Entry<String, ClientInfo> entry : mClients.entrySet()) {
			ClientInfo clientInfo = entry.getValue();
			clientInfo.mClientSender.sendMessage(parts.get(clients).toString());
			clients++;
		}
		return parts;
	}

	/**
	 * Sends given message to all clients in the client list. Actually the message
	 * is added to the client sender thread's message queue and this client sender
	 * thread is notified.
	 * 
	 * @throws InterruptedException
	 */
	private synchronized void sendMessageToClients(String message) {

		if (message.contains("bulletin")) {
			// insert in BDD
			System.out.println("\t(Bulletin reçu : " + message + ")");

			String nin;
			String pin;
			String voteC;

			System.out.println("NIN :");
			nin = message.substring(11, 29);
			System.out.println(nin);

			System.out.println("PIN :");
			pin = message.substring(30, 94);
			System.out.println(pin); // length of PIN is 65
			// NIN Verification

			try {
				Class.forName("com.mysql.jdbc.Driver");
				Connection con = DriverManager.getConnection(
						"jdbc:mysql://localhost:3306/dbvote2?autoReconnect=true&useSSL=false", "root", "projetcrypto");
				Statement stmt = con.createStatement();
				ResultSet rs;
				String query;

				query = "select electeur.NIN, bulletin2.PIN from electeur left join bulletin2 on electeur.id = bulletin2.id;";
				rs = stmt.executeQuery(query);
				while (rs.next()) {
					String NIN = (rs.getString("NIN"));
					System.out.println(NIN);
					String PIN = (rs.getString("PIN"));
					System.out.println(PIN);
					if (nin.equals(NIN)) {
						if (pin.equals(PIN)) {
							System.out.println("Vote chiffré :");
							String pattern = "(\\s)(.*)";

							// Create a Pattern object
							Pattern r = Pattern.compile(pattern);

							// Now create matcher object.
							Matcher m = r.matcher(message.substring(95, message.length()));
							if (m.find()) {
								voteC = m.group(0);
								System.out.println(voteC.substring(1, voteC.length()));
							}

						} else {
							System.out.println("PIN is incorrect");
						}
					} else {
						System.out.println("You don't have the right to vote");
					}
				}

			} catch (Exception e) {
				System.out.println(e);
			}

		} else if (message.contains("init")) {
			// 3rd mod
			if (mClients.size() == 2) {
				System.out.println("Secret à partager : ");
				String privateKey = myKey.key_private.toString();
				System.out.println(privateKey);
				partsSK = distributeKey(privateKey);

				ArrayList<HashMap[]> popol;

				popol = Decompte.compteVote();

				System.out.println("Listes electorales pré-Depouillement : ");
				for (int i = 0; i < 5; i++) {
					System.out.println("Wilaya " + i);
					for (int j = 0; j < 5; j++) {
						System.out.println("Liste  " + j + " | " + popol.get(i)[j]);
					}
					System.out.println("------------------------------");
				}
				System.out.println("||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");

			}

		} else if (message.contains("[B")) {
			// 4th mod
			receivedSK.add(message);
			System.out.println("\t(Partage de clé reçu : " + message + ")");

		} else {
			// message in wrong format, send back to sender
			String newMsg = ":err MESSAGE IS NOT IN \"To receiver's alias: message\" FORMAT! PLEASE REDO!";
			// String receiverAlias = senderAlias;
			// ClientInfo receiver = mClients.get(receiverAlias);
			// receiver.mClientSender.sendMessage(newMsg);
		}
	}

	/**
	 * Infinitely reads messages from the queue and dispatch them to all clients
	 * connected to the server.
	 */
	public void run() {
		try {
			while (true) {

				String message = getNextMessageFromQueue();
				sendMessageToClients(message);

				if (receivedSK.size() == 2) {
					// Reconstruct Secret
					recoveredPrivateKey = scheme.join(partsSK);
					System.out.println("Secret Reconstruit : ");
					System.out.println(new String(recoveredPrivateKey, StandardCharsets.UTF_8));

					// Example of decrypted vote
					DecryptVote.DecryptV();

					// Procede to Tally
					/*
					 * On a ici 5 wilaya ayant chacune 5 listes electorales (donc 5 partis, chaque
					 * parti possède une liste pour chaque wilaya), Nous avons donc 5 liste par
					 * wilaya contenant chacune 3 candidats ce qui nous fait un total de 75
					 * candidats (5wilaya * 5listes * 3candidat)
					 */
					int[] voteListe;
					ArrayList<HashMap[]> popol;

					popol = Decompte.compteVote();

					// You could also decrypt at this point, by writing over the votes with
					// decrypted values after selecing them
					popol = Decompte.Depouillement();

					System.out.println("Listes electorales post-Depouillement : ");
					for (int i = 0; i < 5; i++) {
						System.out.println("Wilaya " + i);
						for (int j = 0; j < 5; j++) {
							System.out.println("Liste  " + j + " | " + popol.get(i)[j]);
						}
						System.out.println("------------------------------");
					}
					System.out.println("||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||");

					voteListe = Decompte.UpdateNbvoteListe(popol);
					Decompte.AssignerSiege(voteListe, popol);
				}
			}
		} catch (InterruptedException e) {
			System.err.print(e);
		}
	}

	public void checkCommand(ClientInfo aClientInfo) throws IOException {
		Socket socket = aClientInfo.mSocket;
		String senderIP = socket.getInetAddress().getHostAddress();
		String senderPort = "" + socket.getPort();
		DataInputStream in = null;
		DataOutputStream out = null;

		// Connect to Nakov Chat Client
		try {
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			System.out.println("******************* Start command check for " + senderIP + ":" + senderPort
					+ " *******************");
		} catch (IOException ioe) {
			aClientInfo.mSocket.close();
			throw new IOException(":fail FAILED TO ESTABLISH SOCKET CONNECTION BETWEEN CHAT HUB AND " + senderIP + ":"
					+ senderPort + "\n");
		}

		// ******************* PHASE 1 initial state ******************* //
		String receivedCipherSuite = null;
		while (receivedCipherSuite == null) {
			// ***** PHASE 1.1: receive :ka cipherSuite ***** //
			try {
				receivedCipherSuite = in.readUTF();
				System.out.println("PHASE 1.1 " + receivedCipherSuite);
			} catch (IOException err) {
				aClientInfo.mSocket.close();
				throw new IOException(":fail NO RESPONSE FROM CLIENT!");
			}

			// check whether received command equals to :ka
			String ka = Help.getCommand(receivedCipherSuite);
			try {
				Help.commandEqual(ka, ":ka");
			} catch (ErrorException fail) {
				aClientInfo.mSocket.close();
				System.err.println(fail);
				throw new IOException();
			}

			// check whether received cipher suite include server cipher suite
			String clientCipher = Help.getCipherSuite(receivedCipherSuite);
			try {
				Help.findCipherSuite(myCipherSuite, clientCipher);
			} catch (ErrorException fail) {
				aClientInfo.mSocket.close();
				System.err.println(fail);
				throw new IOException();
			}

			finally {
				// ***** PHASE 1.2: send :kaok cipherSuite ***** //
				System.out.println("PHASE 1.2 :kaok " + myCipherSuite);
				try {
					out.writeUTF(":kaok " + myCipherSuite);
					out.flush();
				} catch (IOException ioe) {
					aClientInfo.mSocket.close();
					throw new IOException(":fail FAILED TO SEND :kaok CIPHERSUITE TO CLIENT!\n");
				}

				// ***** PHASE 1.3: send :cert based64 encoded certificate *****//
				try {
					byte[] EncodedMyCert = Help.getCert(myKeyStore, myAlias);
					byte[] certEncodedMyCert = Help.addCommand(":cert ", EncodedMyCert);
					System.out.println("PHASE 1.3 :cert " + EncodedMyCert.toString());
					out.writeInt(certEncodedMyCert.length);
					out.write(certEncodedMyCert);
					out.flush();
				} catch (IOException fail) {
					aClientInfo.mSocket.close();
					throw new IOException(":fail FAILED TO SEND :cert ENCODED CERTIFICATE TO CLIENT!\n");
				} catch (ErrorException fail) {
					aClientInfo.mSocket.close();
					System.err.println(fail);
					throw new IOException();
				}

				// ***** PHASE 1.3: send :ka1 based64 encoded public key *****//
				byte[] encodedPublic = myKey.getEncodedPublic();
				byte[] ka1encodedPublic = Help.addCommand(":ka1 ", encodedPublic);
				System.out.println("PHASE 1.3 :ka1 " + encodedPublic.toString());
				try {
					out.writeInt(ka1encodedPublic.length);
					out.write(ka1encodedPublic);
					out.flush();
				} catch (IOException fail) {
					aClientInfo.mSocket.close();
					throw new IOException(":fail FAILED TO SEND :ka1 ENCODED PUBLIC KEY TO CLIENT!\n");
				}
			}
		}

		// ******************* PHASE 3: waiting for key agreement ******************* //
		int receiveSize = 0;
		while (receiveSize == 0) {
			// ***** PHASE 3.1: receive :cert server's encoded certificate ***** //
			byte[] certEncodedCert = null;
			try {
				receiveSize = in.readInt();
				certEncodedCert = new byte[receiveSize];
				in.readFully(certEncodedCert);
				System.out.println("PHASE 3.1 :cert " + certEncodedCert.toString());
			} catch (IOException err) {
				aClientInfo.mSocket.close();
				throw new IOException(":fail NO RESPONSE FROM CLIENT!");
			}

			// ***** PHASE 3.1: verify :cert server's encoded certificate ***** //
			try {
				byte[] encodedCert = Help.splitCommand(":cert ", certEncodedCert);
				String clientAlias = Help.getAlias(myKeyStore, encodedCert, integrity);
				Help.certVerify(myKeyStore, clientAlias, encodedCert, integrity);
				System.out.println(
						"PHASE 3.1 :cert " + encodedCert.toString() + " is verified (from " + clientAlias + ")");
				aClientInfo.mAlias = clientAlias;
			} catch (ErrorException fail) {
				aClientInfo.mSocket.close();
				System.err.println(fail);
				throw new IOException();
			}

			// ***** PHASE 3.1: receive :ka1 client's encoded public key ***** //
			byte[] ka1clientPublic = null;
			try {
				receiveSize = in.readInt();
				ka1clientPublic = new byte[receiveSize];
				in.readFully(ka1clientPublic);
				System.out.println("PHASE 3.1 :ka1 " + ka1clientPublic.toString());
			} catch (IOException err) {
				aClientInfo.mSocket.close();
				throw new IOException(":fail NO RESPONSE FROM CLIENT!");
			}

			// check whether received command equals to :ka1
			byte[] ka1 = Help.getCommand(":ka1 ", ka1clientPublic);
			try {
				Help.commandEqual(ka1, ":ka1 ");
			} catch (ErrorException fail) {
				aClientInfo.mSocket.close();
				System.err.println(fail);
				throw new IOException();
			} finally {
				// ***** PHASE 3.2: generate shared secret ***** //
				try {
					myKey.doECDH(Help.splitCommand(":ka1 ", ka1clientPublic));
					System.out.println("PHASE 3.2 share key: " + myKey.getSecret());
				} catch (ErrorException fail) {
					aClientInfo.mSocket.close();
					System.err.println(fail);
					throw new IOException();
				}
			}
		}

		// ******************* PHASE 4: chat w/ msg encryption ******************* //
		try {
			// initialize Encryption object
			aClientInfo.mEncrption = new Encryption(myKey.getSecret(), symCipher);
			System.out.println("******************* Finish command check for " + senderIP + ":" + senderPort
					+ " *******************");
		} catch (ErrorException fail) {
			aClientInfo.mSocket.close();
			System.err.println(fail);
			throw new IOException();
		}
	}

	/**
	 * initialize KeyExchange object myKey with given cipher suite and ClientInfo
	 * 
	 * @param the cipher suite sent by client, and ClientInformation
	 */
	public void makeMyKey() throws ErrorException {
		myKey = new KeyExchange(keyEstAlgor, keyEstSpec, integrity);
	}

	/**
	 * initialize KeyExchange object myKey with given cipher suite and ClientInfo
	 * 
	 * @param the cipher suite sent by client, and ClientInformation
	 */
	public void getMyKeyStore(String keystoreFileName, String password) throws ErrorException {
		// initialize myAlias from the key store name (alias.jks)
		String[] temp = keystoreFileName.split("\\.");
		myAlias = temp[0];

		// initialize myKeyStore from the key store name
		keystoreFileName = System.getProperty("user.dir") + "/" + keystoreFileName;
		myKeyStore = Help.linkKeyStore(keystoreFileName, password);
	}

}