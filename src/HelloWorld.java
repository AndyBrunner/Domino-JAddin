public class HelloWorld extends JAddinThread {

	// Declarations
	boolean threadRunning = true;
	
	// This is the main entry point. When this method returns, the add-in terminates.
	public void addinStart() {
		
		logMessage("Started with parameters " + getAddinParameters());
		
		try {
			logMessage("Running on " + dbGetSession().getNotesVersion());
		} catch (Exception e) {
			logMessage("Unable to get Domino version: " + e.getMessage());
		}
		// Main add-in loop
		while (threadRunning) {
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
		threadRunning = false;
	}
	
	// This method is called by the JAddin framework for any console command entered.
	@Override
	public void addinCommand(String command) {
		logMessage("You have entered the command " + command);
	}
}
