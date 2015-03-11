public class HelloWorld extends JAddinThread {

	// This method runs in a separate thread to avoid any processing delays of the main Domino message queue.
	public void addinStart() {
		
		logMessage("HelloWorld started with the parameters <" + getAddinParameters() + '>');
		
		try {
			logMessage("Running on " + getDominoSession().getNotesVersion());
		} catch (Exception e) {
			logMessage("Unable to get Domino version: " + e.getMessage());
		}

		// Loop to see that the user thread is running ...
		while (true) {
			logMessage("User code is executing ...");
			waitSeconds(15);
		}
	}

	// This method is called asynchronously by the JAddin framework when the command 'Quit' or 'Exit' is entered or at Domino
	// server shutdown. It should be executed as quickly as possible to avoid any main Domino message queue delays. After
	// returning, the JAddin framework issues Thread.interrupt() to signal termination to the addInStart() code.
	public void addinStop() {
		logMessage("Termination in progress");
	}
	
	// This method is called asynchronously by the JAddin framework for any console command entered. It should be executed as quickly
	// as possible to avoid any main Domino message queue delays.
	public void addinCommand(String command) {
		logMessage("You have entered the command <" + command + '>');
		
		if (command.equalsIgnoreCase("SendMail")) {
			try {
				sendMessage("FromAddress@acme.com", "RecipientAddress@acme.com", "Test message", "Some email text content");
			} catch (Exception e) {
				logMessage("Unable to send message: " + e.getMessage());
			}
		}
	}
}
