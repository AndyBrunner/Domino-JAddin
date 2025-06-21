public class HelloWorld extends JAddinThread {

	// Declarations
	boolean mustTerminate = false;
	
	// This is the main entry point. When this method returns, the add-in terminates.
	public void addinStart() {
		
		logMessage("Started");
		
		// Stay in main loop until main thread JAddin signals termination by calling addinStop() or issued Thread.interrupt()
		while (!addinInterrupted() && !mustTerminate) {
			logMessage("User code is executing...");
			waitMilliSeconds(5000L);
		}
		
		logMessage("Terminated");
	}

	// This method is called by the JAddin main thread when the console command 'Quit' or 'Exit' is entered or during
	// Domino server shutdown. Here you must signal the addinStart() method to terminate itself and to perform any cleanup.
	public void addinStop() {
		logMessage("Termination in progress");
		mustTerminate = true;
	}
	
	// This method is called by the JAddin main thread for any console command entered. It should return quickly to
	// avoid blocking the Domino message queue.
	@Override
	public void addinCommand(String command) {
		logMessage("Command entered: " + command);
	}
}
