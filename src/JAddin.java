import java.lang.reflect.Method;
import java.util.Calendar;

import lotus.notes.addins.JavaServerAddin;
import lotus.notes.internal.MessageQueue;

/**
 * This JAddin class is a framework used to create Lotus Domino server addin tasks.
 *
 * The code running in this thread should avoid any long-running or blocking code to prohibit delays in processing of the Domino
 * message queue.
 * 
 * @author	ABData, Andy Brunner
 * @version	1.2.1 - 03/May/2014
 * 
 * @see		<a href="http://abdata.ch/JAddin.html">Homepage of JAddin</a>
 */
public final class JAddin extends JavaServerAddin {
	
	static	String			JADDIN_VERSION		= "1.2.1";			// TODO
	
	// Instance variables
	private JAddinThread	xJAddinThread		= null;
	private String			xAddinName			= null;
	private	String			xUserClassName		= null;
	private	String			xUserClassParameter	= null;
	private String[]		xCommandLineArgs	= null;
	private MessageQueue	xMessageQueue		= null;
	private int 			xTaskID				= 0;
	private boolean			xDebugEnabled		= false;
	private boolean			xCleanupDone		= false;
	
	/**
	 * This constructor is called by the Domino's RunJava task if no arguments are specified (e.g. <code>"Load RunJava JAddin"</code>).
	 */
	public JAddin() {
	}
		
	/**
	 * This constructor is called by the Domino's RunJava task if command line arguments are specified (e.g. <code>"Load RunJava JAddin HelloWorld"</code>).
	 * 
	 * @param	args	Command line arguments
	 */
	public JAddin(String[] args) {
		xCommandLineArgs = args;
	}
	
	/**
	 * This method is called by the Domino's RunJava task as the main entry point. Since it executes under the main JAddin Domino server
	 * thread, this method creates a subthread based on JAddinThread running the user code. 
	 */
	public final void runNotes() {
	
		// Get the name of the current Java class)
		xAddinName = this.getClass().getName();
		
		// Set the Java thread name to the class name (default is "Thread-n")
		setName(xAddinName);
		
		logDebug("Method " + xAddinName + ".runNotes() called");
		
		if (xCommandLineArgs == null) {
			logMessage("ERROR - Usage is 'Load RunJava JAddin <AddinName> <AddinParameters>'");
			cleanup();
			return;
		}
		
		// Extract the user class name (1st parameter=
		xUserClassName = xCommandLineArgs[0];

		// Create the status line (showed in 'Show Task' command)
		xTaskID = AddInCreateStatusLine("JAddin " + xUserClassName);
		
		// Set the initial state
		setAddinState("Initialization in progress");
		
		// Extract the optional parameters for the user class
		if (xCommandLineArgs.length > 1) {
			xUserClassParameter = "";
			
			for (int index = 1; index < xCommandLineArgs.length; index++) {
				
				// Enable debugging mode if "Debug!" specified
				if (xCommandLineArgs[index].equalsIgnoreCase("Debug!")) {
					setDebugState(true);
					continue;
				}
				
				xUserClassParameter += ' ' + xCommandLineArgs[index];
			}
		}
		
		// Cleanup the command line string
		if (xUserClassParameter != null) {
			xUserClassParameter = xUserClassParameter.trim();
			
			if (xUserClassParameter.length() == 0)
				xUserClassParameter = null;
		}
	
		logDebug("JAddin framework version is " + JADDIN_VERSION);
		
		logDebug("Parameters for the user class are <" + xUserClassParameter + '>');
			
		// Create and open the message queue
		logDebug("Creating the Domino message queue");
		String messageQueueName	= MSG_Q_PREFIX + xUserClassName.toUpperCase();
		xMessageQueue = new MessageQueue();
		int messageQueueState = xMessageQueue.create(messageQueueName, 0, 0);
		
		if (messageQueueState == MessageQueue.ERR_DUPLICATE_MQ) {
			logMessage("ERROR - The " + xUserClassName + " addin task is already running");
			cleanup();
			return;
		}
		
		if (messageQueueState != MessageQueue.NOERROR) {
			logMessage("ERROR - The Domino message queue could not be created");
			cleanup();
			return;
		}
		
		logDebug("Opening the Domino message queue");
		if (xMessageQueue.open(messageQueueName, 0) != NOERROR) {
			logMessage("ERROR - The Domino message queue could not be opened");
			cleanup();
			return;
		}
	
		// Dynamically load the Java class specified in the start parameter of the JAddin task, e.g. "Load RunJava JAddin HelloWorld".
		Class<?>		classClass				= null;
		Method			classAddinInitialize	= null;
		Method			classStart				= null;
		Method			classAddinStop			= null;
		Method			classAddinCommand		= null;
		Method			classAddinNextDay		= null;
		Method			classAddinNextHour		= null;
	
		try {
			logDebug("Loading the user Java class " + xUserClassName);

			classClass					= Class.forName(xUserClassName);
			xJAddinThread				= (JAddinThread) classClass.newInstance();
			classAddinInitialize		= classClass.getMethod("addinInitialize", new Class[] {JAddin.class, String.class});
			classStart					= classClass.getMethod("start", new Class[] {});
			classAddinStop				= classClass.getMethod("addinStop", new Class[] {});
			classAddinCommand			= classClass.getMethod("addinCommand", new Class[] {String.class});
			classAddinNextHour			= classClass.getMethod("addinNextHour", new Class[] {});
			classAddinNextDay			= classClass.getMethod("addinNextDay", new Class[] {});

			logDebug("User Java class " + xUserClassName + " successfully loaded");
		}
		catch (NoClassDefFoundError e)
		{
			logMessage("ERROR - Unable to load the Java class " + xUserClassName + ": " + e.getMessage());
			logMessage("Usage is 'Load RunJava JAddin <AddinName> <AddinParameters>'");
			logMessage("Make sure you enter the addin name with correct upper- and lowercase characters");
			cleanup();
			return;
		}
		catch (Exception e)
		{
			logMessage("ERROR - Unable to load the Java class " + xUserClassName + ": " + e.getMessage());
			logMessage("Usage is 'Load RunJava JAddin <AddinName> <AddinParameters>'");
			logMessage("Make sure you enter the addin name with correct upper- and lowercase characters");
			cleanup();
			return;
		}

		// Call the addInInitialize method
		try {
			logDebug("Calling " + xUserClassName + ".addinInitialize()");
			classAddinInitialize.invoke(xJAddinThread, new Object[] {this, xUserClassParameter});
			logDebug("Returning from " + xUserClassName + ".addinInitialize()");
		} catch (Exception e) {
			logMessage("ERROR - Unhandled exception in " + xUserClassName + ".addinInitialize(): " + e.getMessage());
			
			if (xDebugEnabled)
				e.printStackTrace();
			
			cleanup();
			return;
		}
		
		// Call the start method
		try {
			logDebug("Calling " + xUserClassName + ".start()");
			classStart.invoke(xJAddinThread, new Object[] {});
			logDebug("Returning from " + xUserClassName + ".start()");
		} catch (Exception e) {
			logMessage("ERROR - Unhandled exception in " + xUserClassName + ".start(): " + e.getMessage());
			
			if (xDebugEnabled)
				e.printStackTrace();
			
			cleanup();
			return;
		}
	
		// Main loop
		String		commandLine			= null;
		Runtime		runtime				= Runtime.getRuntime();
		Calendar	lastDate			= Calendar.getInstance();
		Boolean		lowMemoryWarning	= false;
		
		while (true) {
			// Wait for next command on the Domino message queue
			commandLine = getCommand();
			
			// Check if command "Help!" entered
			if (commandLine.equalsIgnoreCase("Help!")) {
				logMessage("The following JAddin commands are available:");
				logMessage("Version!    Display JAddin, Java and OS version numbers");
				logMessage("Quit!       Terminate the addin");
				logMessage("Memory!     Show the Java virtual machine memory usage");
				logMessage("GC!         Executes the Java virtual machine garbage collector");
				logMessage("Debug!      Enable the debug logging to the console");
				logMessage("NoDebug!    Disable the debug logging to the console");
				logMessage("Heartbeat!  Start the heartbeat processing (automatically done every 15 seconds)");				
				continue;
			}
			
			// Check if addin shutdown needed (Quit, Exit, Domino server shutdown or error)
			if (commandLine.equalsIgnoreCase("Quit!")) {
				
				logDebug(xUserClassName + " termination in progress");
				
				// Call the user addInStop method
				try {
					logDebug("Calling " + xUserClassName + ".addinStop()");
					classAddinStop.invoke(xJAddinThread, new Object[] {});
					logDebug("Returning from " + xUserClassName + ".addinStop()");
				} catch (Exception e) {
					logMessage("ERROR - Unhandled exception in " + xUserClassName + ".addinStop(): " + e.getMessage());
					
					if (xDebugEnabled)
						e.printStackTrace();
					
					cleanup();
				}
				
				// Interrupt the subthread
				logDebug("Interrupting the subthread");
				if (isJAddinThreadAlive())
					xJAddinThread.interrupt();
				
				// Wait maximum 3 second for subthread termination
				if (isJAddinThreadAlive()) {

					logDebug("Waiting for subthread termination");
				
					for (int index = 0; index < 12; index++) {

						if (!isJAddinThreadAlive()) {
							logDebug("Subthread has terminated itself");
							break;
						}
							
						// Wait 0.25 seconds
						waitMilliSeconds(250L);
					}
				}
								
				break;
			}
			
			// Check if command "Version!" entered
			if (commandLine.equalsIgnoreCase("Version!")) {
				logMessage("JAddin Version:    " + JADDIN_VERSION);
				logMessage("Java VM Version:   " + System.getProperty("java.version", "n/a") + " (" + System.getProperty("java.vendor", "n/a") + ")");
				logMessage("System OS Version: " + System.getProperty("os.version", "n/a") + " (" + System.getProperty("os.name", "n/a") + ")");
				continue;
			}
			
			// Check if command "Debug!" entered
			if (commandLine.equalsIgnoreCase("Debug!")) {
				xDebugEnabled = true;
				logMessage("Debug logging is now enabled");
				continue;
			}
			
			// Check if command "NoDebug!" entered
			if (commandLine.equalsIgnoreCase("NoDebug!")) {
				xDebugEnabled = false;
				logMessage("Debug logging is now disabled");
				continue;
			}
			
			// Check if command "GC!" entered
			if (commandLine.equalsIgnoreCase("GC!")) {
				callJavaGC();
				continue;
			}
			
			// Check if command "Memory!" entered
			if (commandLine.equalsIgnoreCase("Memory!")) {
				
				long memoryMax				= runtime.maxMemory();
				long memoryUsed				= runtime.totalMemory() - runtime.freeMemory();
				long memoryUsedPercent		= (memoryUsed * 100L) / memoryMax;
				
				logMessage("JVM memory usage: Configured " + (memoryMax / 1024L) + " KB, Used " + (memoryUsed / 1024L) + " KB (" + memoryUsedPercent + " %%)");
				continue;
			}
			
			// Check if command "Heartbeat!" entered
			if (commandLine.equalsIgnoreCase("Heartbeat!")) {
				
				logDebug("Performing hearbeat checks");
				// Check if the subthread has completed
				if (!isJAddinThreadAlive()) {
					logMessage("ERROR - Abnormal completion of addin " + xUserClassName + " detected");
					cleanup();
					break;
				}
				
				// Check if JVM heap space too small
				long	memoryMax			= runtime.maxMemory();
				long	memoryUsed			= runtime.totalMemory() - runtime.freeMemory();
				long	memoryFree			= memoryMax - memoryUsed;
				double	memoryUsedPercent	= (memoryUsed * 100.0) / memoryMax;
				
				if (memoryUsedPercent > 90.0) {
					// Show warning message once for each threshold reached
					if (!lowMemoryWarning) {
						logMessage("WARNING: The free Java heap space is below 10 percent (" + (memoryFree / 1024) + " KB free");
						logMessage("Consider adding more memory thru the Notes.Ini variable <JavaMaxHeapSize=xxxxMB>");
						lowMemoryWarning = true;
					}
					callJavaGC();
				} else
					lowMemoryWarning = false;

				Calendar currentDate = Calendar.getInstance();

				// Check if next hour
				if (currentDate.get(Calendar.HOUR_OF_DAY) != lastDate.get(Calendar.HOUR_OF_DAY)) {
					// Call the user addinNextHour method
					try {
						logDebug("Calling " + xUserClassName + ".addinNextHour()");
						classAddinNextHour.invoke(xJAddinThread, new Object[] {});
						logDebug("Returning from " + xUserClassName + ".addinNextHour()");
					} catch (Exception e) {
						logMessage("ERROR - Unhandled exception in " + xUserClassName + ".addinNextHour(): " + e.getMessage());
						
						if (xDebugEnabled)
							e.printStackTrace();
					}
				}
				
				// Check if next day
				if (currentDate.get(Calendar.DAY_OF_MONTH) != lastDate.get(Calendar.DAY_OF_MONTH)) {
					// Call the user addinNextDay method
					try {
						logDebug("Calling " + xUserClassName + ".addinNextDay()");
						classAddinNextDay.invoke(xJAddinThread, new Object[] {});
						logDebug("Returning from " + xUserClassName + ".addinNextDay()");
					} catch (Exception e) {
						logMessage("ERROR - Unhandled exception in " + xUserClassName + ".addinNextDay(): " + e.getMessage());
						
						if (xDebugEnabled)
							e.printStackTrace();
					}
				}
				
				lastDate = currentDate;
				
				continue;
			}
			
			try {
				logDebug("Calling " + xUserClassName + ".addinCommand()");
				classAddinCommand.invoke(xJAddinThread, new Object[] {new String(commandLine)});
				logDebug("Returning from " + xUserClassName + ".addinCommand()");
			} catch (Exception e) {
				logMessage("ERROR - Unhandled exception in " + xUserClassName + ".addinCommand(): " + e.getMessage());
				
				if (xDebugEnabled)
					e.printStackTrace();
				
				cleanup();
				break;
			}
		}
		
		// Cleanup
		cleanup();
		
		// Terminate this add-in task and return to the RunJava task
		return;
	}
	
	/**
	 * Set the debug state.
	 * 
	 * @param	pDebugState	Enable or disable the debug log. 
	 */
	final void setDebugState(boolean pDebugState) {
		xDebugEnabled = pDebugState;
	}
	
	/**
	 * Returns the debug state.
	 * 
	 * @return	boolean Debug state 
	 */
	final boolean getDebugState() {
		return xDebugEnabled;
	}
	
	/**
	 * Returns the passed command arguments (e.g. <code>"Load RunJava Addin AddinName xxxxxxxx"</code>).
	 * 
	 * @return	Command line arguments or null
	 */
	final String[] getStartArguments() {
		return xCommandLineArgs;
	}
	
	/**
	 * Waits for the next command from the Domino console and returns it.
	 * 
	 * @return	Entered command or "Quit!" (for "Quit", "Exit", Domino shutdown or errors). The special "Heardbeat!" command is 
	 * returned after every 15 seconds to allow for some housekeeping processing. 
	 */
	final private String getCommand() {
		logDebug("Method getCommand() called");
		
		StringBuffer commandLine	= new StringBuffer(1024);
		int	messageQueueState		= 0;
		
		// Get next command from the queue or timeout after 15 seconds
		messageQueueState = xMessageQueue.get(commandLine, 1024, MessageQueue.MQ_WAIT_FOR_MSG, 15000);
		
		// Quit or Exit (implicit)
		if (messageQueueState == MessageQueue.ERR_MQ_QUITTING) {
			logDebug("User entered 'Quit'/'Exit' or server shutdown in progress");
			return "Quit!";				
		}

		// Check if heartbeat timeout
		if (messageQueueState == MessageQueue.ERR_MQ_TIMEOUT) {
			logDebug("Signalling heartbeat processing");
			return "Heartbeat!";		
		}

		// Check if errors reading the message queue
		if (messageQueueState != NOERROR) {
			logMessage("ERROR - Unable to read command from the Domino message queue");
			return "Quit!";				
		}
		
		logDebug("User entered the command <" + commandLine + '>');

		// Return the stripped command
		return commandLine.toString().trim();
	}
	
	/**
	 * Set the addin status message text. This text is shown in response to the Domino console command <code>"show tasks"</code>.
	 * 
	 * @param 	pMessage	Status message or null to set the string to "Idle"
	 */
	final void setAddinState(String pMessage) {
		
		logDebug("Method setAddinState() called with <" + pMessage + ">");
		
		// Return if the task line is not yet created
		if (xTaskID == 0)
			return;
		
		if (pMessage == null)
			AddInSetStatusLine(xTaskID, "Idle");
		else
			AddInSetStatusLine(xTaskID, pMessage);
	}
	
	/**
	 * Sends Quit command to the Domino message queue to signal the thread to terminate.
	 */
	final void sendQuitCommand() {
		logDebug("Sending 'Quit' command to Domino message queue");
		xMessageQueue.putQuitMsg();
	}
	
	/**
	 * Calls the Java virtual machines garbage collector.
	 */
	final private void callJavaGC() {
		
		Runtime runtime = Runtime.getRuntime();

		logDebug("Calling the Java virtual machine collector");
		long heapFreeMBStart = (runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory()) / 1024);
		System.gc();
		long heapFreeMBStop = (runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory()) / 1024);
		
		logMessage("The JVM garbage collector reclaimed " + (heapFreeMBStop - heapFreeMBStart) + " KB memory");
	}

	/**
	 * Write a log message to the Domino console. The message string will be prepended with the addin name followed by a column,
	 * e.g. <code>"AddInName: xxxxxxxx"</code>
	 * 
	 * @param 	pMessage	Message to be displayed
	 */
	final private void logMessage(String pMessage) {
		AddInLogMessageText(xAddinName + ": " + pMessage, 0);
	}
	
	/**
	 * Write a log message to the Domino console. The message string will be prepended with the addin name followed by a column,
	 * e.g. <code>"AddInName: xxxxxxxx"</code>
	 * 
	 * @param	pNname	Name of the addin (if called from JAddinThread)
	 * @param 	pMessage	Message to be displayed
	 */
	final void logMessage(String pName, String pMessage) {
		AddInLogMessageText(pName + ": " + pMessage, 0);
	}
	
	/**
	 * Write debug message to the standard output (Domino console). The message string will be prepended by Domino
	 * and by this method with e.g. <code>"RunJava JVM: AddInName(ThreadID).MethodName(LineNumber): xxxxxxxx"</code>.
	 * The message is only logged if the debug flag is enabled thru <code>setDebugState(true)</code> or thru the Domino command
	 * "Tell AddinName Debug!". 
	 * 
	 * @param	pMessage	Message to be displayed
	 * 
	 * @see		#setDebugState 			 
	 */
	private final void logDebug(String pMessage) {
		
		if (xDebugEnabled) {
			// Format thread id
			String classMethod = "" + '[' + Thread.currentThread().getId() + ']';
    
			// Get calling method name
			StackTraceElement stackTraceElements[] = Thread.currentThread().getStackTrace();
		    
			if (stackTraceElements.length > 3)
				classMethod += '.' + stackTraceElements[3].getMethodName() + '(' + stackTraceElements[3].getLineNumber() + ')';

			System.out.println(xAddinName + classMethod + ": " + pMessage);
		}
	}
	
	/**
	 * Wait some time.
	 * 
	 * @param	pWaitTime Wait time in milliseconds (0.00x seconds) 
	 */
	final private void waitMilliSeconds(long pWaitTime) {
		try {
			Thread.sleep(pWaitTime);
		} catch (Exception e) {
			logMessage("ERROR - Unable to delay the thread for " + pWaitTime + " ms: " + e.getMessage());
		}
	}

	/**
	 * Return state of JAddinThread
	 * 
	 * @return 	Indicator
	 * 	 */
	final private boolean isJAddinThreadAlive() {
		
		if (xJAddinThread == null)
			return false;
		
		if (!xJAddinThread.isAlive())
		{
			xJAddinThread = null;
			return false;
		}
		
		return true;
	}

	/**
	 * Performs all necessary cleanup tasks. 
	 */
	@SuppressWarnings("deprecation")
	private final void cleanup() {
		
		logDebug("Method cleanup() called");
		
		// Check if cleanup already done
		if (xCleanupDone)
			return;
		
		// Kill the subthread
		if (isJAddinThreadAlive()) {
			logDebug("Killing the subthread");
			
			// TODO I really don't know of any better way to ultimately stop the sub-thread
//			xJAddinThread.suspend();
//			xJAddinThread.stop();
			
			// Wait until sub-thread terminates
			while (isJAddinThreadAlive())
				waitMilliSeconds(250L);
			
			logDebug("Subthread terminated");
		}
		
		try {
			logDebug("Freeing the Domino resources");
			
			// Close status line
			if (xTaskID != 0) {
				AddInDeleteStatusLine(xTaskID);
				xTaskID = 0;
			}
			
			// Close message queue
			if (xMessageQueue != null) {
				xMessageQueue.close(0);
				xMessageQueue = null;	
			}	
		} catch (Exception e) {
			logMessage("ERROR - Cleanup processing failed: " + e.getMessage());
		}
		
		xCleanupDone = true;
	}
	
	/**
	 * This method is called by the Java runtime during garbage collection to free all resources owned by this object.
	 */
	public void finalize() {
		
		logDebug("Method finalize() called");
		
		// Free all resources if necessary
		cleanup();
		
		// Call the superclass method
		super.finalize();
	}
}


