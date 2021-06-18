package secure.chat;

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
			Help.greeting(mAlias);
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			while (!isInterrupted()) {
				// read input from keyboard
				message = in.readLine();

				if (message.contains("res")) {
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

				} else if (message.contains("req")) {
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
