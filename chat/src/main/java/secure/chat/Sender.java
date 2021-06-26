package secure.chat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nakov Chat Client
 * (c) Svetlin Nakov, 2002
 * 
 * Sender thread reads messages from the standard input and sends them
 * to the server.
 */
import java.io.*;

public class Sender extends Thread {
	private PrintWriter mOut;
	private Encryption mCov;
	private String mAlias;

	private String mySharedKey;

	public Sender(PrintWriter aOut, Encryption aCov, String alias) {
		mOut = aOut;
		mCov = aCov;
		mAlias = alias;
	}

	// should input ClientInfo aClientInfo
	public synchronized void respSharedKey(String SharedKey) {
		mySharedKey = SharedKey;
	}

	/**
	 * Until interrupted reads messages from the standard input (keyboard) and sends
	 * them to the chat server through the socket.
	 */
	public void run() {
		String message = null;
		String ciphertext = null;
		try {
			Help.prevote(mAlias);

			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			while (!isInterrupted()) {
				// read input from keyboard
				message = in.readLine();

				if (message.contains("send")) {
					try {
						// encrypt the input plaintext into ciphertext
						ciphertext = mCov.encrypt(mySharedKey);
						System.out.println("\t(Encrypted shared key: " + ciphertext + ")");
					} catch (ErrorException fail) {
						System.err.println(fail);
						System.exit(-1);
					}

					// send the ciphertext to chat server through the socket
					mOut.println(ciphertext);
					mOut.flush();
				} else if (message.contains("nin")) {
					try {
						// encrypt the input plaintext into ciphertext
						ciphertext = mCov.encrypt(message);
						System.out.println("\t(Encrypted into cipher text: " + ciphertext + ")");
					} catch (ErrorException fail) {
						System.err.println(fail);
						System.exit(-1);
					}
					// send the ciphertext to chat server through the socket
					mOut.println(ciphertext);
					mOut.flush();

				} else if (message.contains("bulletin")) {
					try {
						String pin;
						String vote = "";
						String voteC;
						String bulletin = "";

						System.out.println("PIN :");
						pin = message.substring(11, 75);
						System.out.println(pin); // length of PIN is 65
						System.out.println("Candidat :");
						String pattern = "(\\d+)";

						// Create a Pattern object
						Pattern r = Pattern.compile(pattern);

						// Now create matcher object.
						Matcher m = r.matcher(message.substring(76, message.length()));
						if (m.find()) {
							vote = m.group(0);
							System.out.println(vote);
							voteC = mCov.encrypt(vote);
							System.out.println("Vote chiffré :");
							System.out.println(voteC);
							bulletin = "bulletin " + pin + " " + voteC;
							ciphertext = mCov.encrypt(bulletin);
						}
						// encrypt the input plaintext into ciphertext
						System.out.println("\t(Encrypted into cipher text: " + ciphertext + ")");
					} catch (ErrorException fail) {
						System.err.println(fail);
						System.exit(-1);
					}
					// send the ciphertext to chat server through the socket
					mOut.println(ciphertext);
					mOut.flush();

				} else if (message.contains("init")) {
					try {
						// encrypt the input plaintext into ciphertext
						ciphertext = mCov.encrypt(message);
						System.out.println("\t(Encrypted into cipher text: " + ciphertext + ")");
					} catch (ErrorException fail) {
						System.err.println(fail);
						System.exit(-1);
					}
					// send the ciphertext to chat server through the socket
					mOut.println(ciphertext);
					mOut.flush();

				} else if (message.equals("exit")) {
					Help.ending(mAlias);
					mOut.close();
					break;
				}

			}

		} catch (IOException err) {
			System.err.println(err);
		}
	}
}
