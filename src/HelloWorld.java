public class HelloWorld extends JAddinThread {

	// Declarations
	boolean terminateThread = false;
	
	// This is the main entry point. When this method returns, the add-in terminates.
	public void addinStart() {
		
		logMessage("Started with parameter: " + getAddinParameters());

		try {
			logMessage("Running on " + dbGetSession().getNotesVersion());
		} catch (Exception e) {
			logMessage("Unable to get Domino version: " + e.getMessage());
		}
		
		// Stay in main loop until termination signal set by addinStop()
		while (!terminateThread) {
			logMessage("User code is executing...");
			waitMilliSeconds(5000L);
		}
		
		logMessage("Terminated");
	}

	// This method is called by the JAddin framework when the command 'Quit' or 'Exit' is entered or during
	// Domino server shutdown. Here you must signal the addinStart() method to terminate itself and to perform any cleanup.
	public void addinStop() {
		
		logMessage("Termination in progress");
		
		// Signal addinStart method to terminate thread
		terminateThread = true;
	}
	
	// This method is called by the JAddin framework for any console command entered.
	@Override
	public void addinCommand(String command) {
		logMessage("You have entered the command: " + command);
	}
}
