import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import lotus.notes.addins.JavaServerAddin;
import lotus.notes.internal.MessageQueue;

/**
 * This JAddin class - together with the JAddinThread class - is a framework used to create HCL Domino
 * server add-in programs.
 *
 * Notes: 	The code running in this thread should avoid any long-running or blocking code to prohibit
 * 			delays in processing the HCL Domino message queue. Some of the methods in this class are also
 * 			called by the JAddinThread and the user add-in class.
 * 
 * @author	andy.brunner@k43.ch
 * 
 * @see		<a href="https://jaddin.k43.ch">Homepage of Domino-JAddin</a>
 */
public final class JAddin extends JavaServerAddin {
	
	// Constants
	static final String		JADDIN_NAME				= "JAddin";
	static final String		JADDIN_VERSION			= "2.2.0";			// Always keep up with the README.md, DOWNLOAD.md and class comments
	static final String		JADDIN_DATE				= "2025-06-16";		// Always keep up with the README.md, DOWNLOAD.md and class comments
	static final String		STAT_OS_VERSION			= "Domino.Platform";
	static final String		STAT_JVM_VERSION		= "JVM.Version";
	
	static final String		STAT_JVM_HEAPDEFINEDKB	= "JVM.HeapLimitKB";
	static final String		STAT_JVM_HEAPUSEDKB		= "JVM.HeapUsedKB";
	static final String		STAT_JADDIN_VERSION		= JADDIN_NAME + ".VersionNumber";
	static final String		STAT_JADDIN_DATE		= JADDIN_NAME + ".VersionDate";
	static final String		STAT_JADDIN_STARTTIME	= JADDIN_NAME + ".StartedTime";

	// Instance variables
	private JAddinThread	jAddinThread			= null;
	private String[]		jAddinArgs				= null;
	private	String			userAddinName			= null;
	private	String			userAddinParameter		= null;
	private MessageQueue	dominoMsgQueue			= null;
	private int 			dominoTaskID			= 0;
	private boolean			jAddinCleanupDone		= false;
	private boolean			startupError			= false;
	private boolean			debugState				= false;
		
	/**
	 * Convert ISO 8601 date string to Java Date
	 * 
	 * @param isoDate Java Date object
	 * @return Formatted date in ISO format ("yyyy-mm-ddThh:mm:ssZ")
	 */
	static final Calendar fromISODate(String isoDate) {

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		Calendar calendar = Calendar.getInstance();
		
		try {
			calendar.setTime(dateFormat.parse(isoDate));
		} catch (Exception e) {
			return null;
		}
		return (calendar);
	}
	/**
	 * Convert Java Date to ISO 8601 UTC string
	 *  
	 * Note: This method is also called by the JAddinThread and the user add-in
	 * 
	 * @param date Java Calendar object
	 * @return Formatted date in ISO format ("yyyy-mm-ddThh:mm:ssZ")
	 */
	static final String toISODateUTC(Calendar date) {
		return (toISODateUTC(date.getTime()));
	}
	/**
	 * Convert Java Date to ISO 8601 UTC string
	 *  
	 * Note: This method is also called by the JAddinThread and the user add-in
	 * 
	 * @param date Java Date object
	 * @return Formatted date in ISO format ("yyyy-mm-ddThh:mm:ssZ")
	 */
	static final synchronized String toISODateUTC(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return (dateFormat.format(date));	
	}

	/**
	 * This constructor is called by the HCL Domino RunJava task if no arguments are specified.
	 * (<code>"Load RunJava JAddin"</code>).
	 */
	public JAddin() {
		logMessage("Usage: 'Load RunJava JAddin <AddinName> [AddinParameters]'");
		this.startupError = true;
	}

	/**
	 * This constructor is called by the HCL Domino RunJava task if any command line arguments is specified.
	 * (<code>"Load RunJava JAddin HelloWorld"</code>).
	 * 
	 * @param 	args[]	Passed arguments in the command line
	 */
	public JAddin(String[] args) {
		this.jAddinArgs = args;
	}

	/**
	 * Check JVM heap space
	 */
	private void checkHeapSpace() {
	
		Runtime	runtime				= Runtime.getRuntime();
		long	memoryMax			= runtime.maxMemory();
		long	memoryUsed			= runtime.totalMemory() - runtime.freeMemory();
		long	memoryUsedPercent	= Math.round((memoryUsed * 100.0) / memoryMax);
		
		if (memoryUsedPercent > 90) {
			logMessage("Warning: Java heap space is " + memoryUsedPercent + "%% used. Consider allocating more memory thru Notes.Ini variable 'JavaMaxHeapSize='");
		}

		// Update statistics
		setDominoStatistic(this.userAddinName, STAT_JVM_HEAPUSEDKB, (double) Math.round(memoryUsed / 1024));
	}
	
	/**
	 * Create the Domino task status line which is shown in <code>"show tasks"</code> command.
	 * 
	 * Note: This method is also called by the JAddinThread and the user add-in
	 * 
	 * @param	name	Name of task
	 * @return	Domino task ID
	 */
	public final int createAddinStatusLine(String name) {
		return (AddInCreateStatusLine(name));
	}

	/**
	 * Delete the Domino task status line.
	 * 
	 * Note: This method is also called by the JAddinThread and the user add-in
	 * 
	 * @param	id	Domino task id
	 */
	public final void deleteAddinStatusLine(int id) {
		if (id != 0) {
			AddInDeleteStatusLine(id);
		}
	}

	/**
	 * Delete the server statistic which is shown in command <code>"Show Stat"</code>.
	 * 
	 * Note: This method is also called by the JAddinThread and the user add-in
	 * 
	 * @param addinName		Name of statistics package
	 * @param statsName		Name of statistics
	 */
	public final void deleteDominoStatistic(String addinName, String statsName) {
		StatDelete(addinName, statsName);
	}

	/**
	 * Wait for the next command from the Domino console and returns it.
	 * 
	 * @return	Entered command or "Quit!" (for "Quit", "Exit", Domino shutdown or errors).
	 */
	private final String getCommand() {

		StringBuffer commandLine = new StringBuffer(1024);
		
		// Get next command from the queue ('Tell <Addin> xxxxxxxx") or timeout after 15 seconds
		int messageQueueState = this.dominoMsgQueue.get(commandLine, 1024, MessageQueue.MQ_WAIT_FOR_MSG, 15000);
		
		// Quit or Exit (implicit)
		if (messageQueueState == MessageQueue.ERR_MQ_QUITTING) {
			logDebug("Termination in progress");
			return "Quit!";				
		}

		// Check if 15 seconds timeout - Return heartbeat request
		if (messageQueueState == MessageQueue.ERR_MQ_TIMEOUT) {
			return "Heartbeat!";		
		}

		// Check if error reading the message queue
		if (messageQueueState != NOERROR) {
			logMessage("Error reading from the Domino message queue");
			return "Quit!";				
		}
		
		String command = commandLine.toString().trim();
		
		logDebug("Domino message queue command: " + command);

		// Return the stripped command
		return command;
	}
		
	/**
	 * Return the debug state
	 * 
	 * Note: This method is also called by the JAddinThread and the user add-in
	 * 
	 * @return	Debug state 
	 */
	public final boolean getDebugState() {
		return this.debugState;
	}
	
	/**
	 * Return the state of JAddinThread.
	 *
	 * @return	Status indicator (active or inactive)
	 */
	private final boolean isJAddinThreadAlive() {
		
		if ((this.jAddinThread != null) && this.jAddinThread.isAlive()) {
			return true;
		}
		
		this.jAddinThread = null;
		return false;
	}
	
	/**
	 * Performs all necessary cleanup tasks
	 */
	private final void jAddinCleanup() {
		
		// Check if cleanup already done
		if (this.jAddinCleanupDone) {
			return;
		}
		
		logDebug("Entered jAddinCleanup()");
		
		// Delete the Domino statistics
		deleteDominoStatistic(this.userAddinName, STAT_OS_VERSION);
		deleteDominoStatistic(this.userAddinName, STAT_JADDIN_VERSION);
		deleteDominoStatistic(this.userAddinName, STAT_JADDIN_DATE);
		deleteDominoStatistic(this.userAddinName, STAT_JVM_VERSION);
		deleteDominoStatistic(this.userAddinName, STAT_JVM_HEAPDEFINEDKB);
		deleteDominoStatistic(this.userAddinName, STAT_JVM_HEAPUSEDKB);
		deleteDominoStatistic(this.userAddinName, STAT_JADDIN_STARTTIME);
		
		// Wait for the user add-in to terminate
		if (isJAddinThreadAlive()) {
			logMessage("Waiting for " + this.userAddinName + " to terminate");
			
			// Wait until thread terminates
			while (isJAddinThreadAlive())
				waitMilliSeconds(125L);
			
			logDebug(this.userAddinName + " has terminated");
		}
		
		try {
			logDebug("Freeing the Domino resources");

			// Delete the Domino task status line (Show Tasks)
			deleteAddinStatusLine(this.dominoTaskID);
			this.dominoTaskID = 0;
			
			// Close message queue
			if (this.dominoMsgQueue != null) {
				this.dominoMsgQueue.close(0);
				this.dominoMsgQueue = null;	
			}	
		} catch (Exception e) {
			logMessage("Unable to cleanup Domino resources: " + e.toString());
		}
		
		this.jAddinCleanupDone = true;
	}
	
	/**
	 * Write a debug message to the Domino console. The message string will be prefixed with the add-in name
	 * and the location in the source code issuing the call e.g. <code>"DEBUG: AddinName.MethodName(LineNumber): xxxxxxxx"</code>.
	 * 
	 * @param	message		Message to be displayed
	 */
	private final void logDebug(String message) {

		if (debugState) {
			logDebug(JADDIN_NAME, message);
		}
	}
	
	/**
	 * Write a debug message to the Domino console. The message string will be prefixed with the add-in name
	 * and the location in the source code issuing the call e.g. <code>"DEBUG: AddinName.MethodName(LineNumber): xxxxxxxx"</code>.
	 * 
	 * Note: This method is also called by the JAddinThread and the user add-in
	 * 
	 * @param 	addinName	Name of Add-in
	 * @param	message		Message to be displayed
	 */
	public final synchronized void logDebug(String addinName, String message) {
		
		if (!this.debugState)
			return;
		
		// Get thread name
		StringBuilder moduleInfo = new StringBuilder(addinName).append('.');
		
		// Get method name and location from the Java calling stack
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
						    
		if (stackTraceElements.length > 3) {
			moduleInfo.append(stackTraceElements[3].getMethodName())
				.append('(')
				.append(stackTraceElements[3].getLineNumber())
				.append(')');
		} else {
			moduleInfo.append("N/A");
		}
				
		// Format module information to fixed wide
		while (moduleInfo.length() < 35) {
			moduleInfo.append(' ');
		}
				
		AddInLogMessageText("DEBUG: " + moduleInfo.substring(0,  35) + ' ' + message, 0);
	}
	
	/**
	 * Write a log message to the Domino console. The message string will be prefixed with the add-in name
	 * followed by a column, e.g. <code>"AddinName: xxxxxxxx"</code>
	 * 
	 * @param	message		Message to be displayed
	 */
	private final void logMessage(String message) {
		AddInLogMessageText(JADDIN_NAME + ": " + message, 0);
	}
	
	/**
	 * Write a log message to the Domino console. The message string will be prefixed with the add-in name
	 * followed by a column, e.g. <code>"AddinName: xxxxxxxx"</code>
	 * 
	 * Note: This method is also called by the JAddinThread and the user add-in
	 * 
	 * @param	addinName	Name of add-in
	 * @param	message		Message to be displayed
	 */
	public final void logMessage(String addinName, String message) {
		AddInLogMessageText(addinName + ": " + message, 0);
	}

	/**
	 * This method is called by the Domino RunJava task as the main entry point.
 	 */
	@Override
	@SuppressWarnings("deprecation")
	public final void runNotes() {
	
		// Terminate immediately if startup has failed
		if (this.startupError)
			return;

		// Set the Java thread name to the class name (default would be "Thread-n")
		setName(JADDIN_NAME);
		
		// Extract the user add-in name (1st parameter)
		this.userAddinName = this.jAddinArgs[0];

		// Set initial Domino statistics
		setDominoStatistic(this.userAddinName, STAT_OS_VERSION, System.getProperty("os.version", "n/a") + " (" + System.getProperty("os.name", "n/a") + ")");
		setDominoStatistic(this.userAddinName, STAT_JADDIN_VERSION, JADDIN_VERSION);
		setDominoStatistic(this.userAddinName, STAT_JADDIN_DATE, JADDIN_DATE);
		setDominoStatistic(this.userAddinName, STAT_JVM_VERSION, System.getProperty("java.version", "n/a") + " (" + System.getProperty("java.vendor", "n/a") + ")");
		setDominoStatistic(this.userAddinName, STAT_JVM_HEAPDEFINEDKB, ((double) Runtime.getRuntime().maxMemory()) / 1024);
		setDominoStatistic(this.userAddinName, STAT_JVM_HEAPUSEDKB, (double) Math.round((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024d));
		setDominoStatistic(this.userAddinName, STAT_JADDIN_STARTTIME, JAddin.toISODateUTC(new Date()));

		// Check heap space
		checkHeapSpace();
		
		// Create the status line showed in 'Show Task' console command
		this.dominoTaskID = createAddinStatusLine(JADDIN_NAME + " Main Task");
		
		// Set the initial state
		setAddinState("Initialization in progress");
		
		// Process special JAddin arguments and extract optional arguments for the user class (2nd thru n-th parameter)
		if (this.jAddinArgs.length > 1) {
			
			this.userAddinParameter = "";
			
			for (int index = 1; index < this.jAddinArgs.length; index++) {
				
				switch ((this.jAddinArgs[index].toLowerCase())) {
					case "debug!": {
						setDebugState(true);
						logMessage("Enter 'Tell " + userAddinName + " NoDebug!' to disable debug logging");
						continue;
					}
					case "nodebug!": {
						setDebugState(false);
						logMessage("Enter 'Tell " + userAddinName + " Debug!' to enable debug logging");
						continue;
					}
					default: {
						// Construct parameter line for thread (without "Debug!" parameter)
						this.userAddinParameter += ' ' + this.jAddinArgs[index];
					}
				}
			}
		}
		
		// Cleanup the command line string
		if (this.userAddinParameter != null) {
			this.userAddinParameter = this.userAddinParameter.trim();
			
			if (this.userAddinParameter.length() == 0)
				this.userAddinParameter = null;
		}
	
		logDebug(JADDIN_NAME + " framework version " + JADDIN_VERSION);
		logDebug("OS platform: " + System.getProperty("os.version", "n/a") + " (" + System.getProperty("os.name", "n/a") + ")");
		logDebug("JVM version: " + System.getProperty("java.version", "n/a") + " (" + System.getProperty("java.vendor", "n/a") + ")");
		logDebug(this.userAddinName + " will be called with parameter: " + this.userAddinParameter);
		
		// Create and open the message queue
		logDebug("Creating and opening the Domino message queue");

		String messageQueueName	= MSG_Q_PREFIX + this.userAddinName.toUpperCase();
		this.dominoMsgQueue = new MessageQueue();
		int messageQueueState = dominoMsgQueue.create(messageQueueName, 0, 0);
		
		if (messageQueueState == MessageQueue.ERR_DUPLICATE_MQ) {
			logMessage(this.userAddinName + " task is already running");
			jAddinCleanup();
			return;
		}
		
		if (messageQueueState != MessageQueue.NOERROR) {
			logMessage("Unable to create the Domino message queue");
			jAddinCleanup();
			return;
		}
		
		if (this.dominoMsgQueue.open(messageQueueName, 0) != NOERROR) {
			logMessage("Unable to open Domino message queue");
			jAddinCleanup();
			return;
		}

		// Dynamically load the class specified in the start parameter, e.g. "Load RunJava JAddin HelloWorld".
		Class<?>	classClass				= null;
		Method		classAddinInitialize	= null;
		Method		classStart				= null;
		Method		classAddinStop			= null;
		Method		classAddinCommand		= null;
		Method		classAddinNextDay		= null;
		Method		classAddinNextHour		= null;

		try {
			logDebug("Loading Java class " + this.userAddinName);

			classClass				= Class.forName(this.userAddinName);
			this.jAddinThread		= (JAddinThread) classClass.newInstance();
			classAddinInitialize	= classClass.getMethod("addinInitialize", new Class[] {JAddin.class, String.class});
			classStart				= classClass.getMethod("start", new Class[] {});
			classAddinStop			= classClass.getMethod("addinStop", new Class[] {});
			classAddinCommand		= classClass.getMethod("addinCommand", new Class[] {String.class});
			classAddinNextHour		= classClass.getMethod("addinNextHour", new Class[] {});
			classAddinNextDay		= classClass.getMethod("addinNextDay", new Class[] {});
		}
		catch (Exception e)
		{
			logMessage("Unable to load Java class " + this.userAddinName);
			logMessage("Make sure add-in is in correct directory and name is written with correct upper-/lowercase");
			jAddinCleanup();
			return;
		}

		// Call the addInInitialize(this, arguments) method
		try {
			logDebug("Calling " + this.userAddinName + ".addinInitialize()");
			classAddinInitialize.invoke(this.jAddinThread, new Object[] {this, this.userAddinParameter});
		} catch (Exception e) {
			logMessage("Unhandled exception in " + this.userAddinName + ".addinInitialize(): " + e.toString());
			jAddinCleanup();
			return;
		}
		
		// Call the start() method (part of JavaServerAddin) which will then call runNotes()
		try {
			logDebug("Calling " + this.userAddinName + ".start()");
			classStart.invoke(this.jAddinThread, new Object[] {});
		} catch (Exception e) {
			logMessage("Unhandled exception in " + this.userAddinName + ".start(): " + e.toString());
			jAddinCleanup();
			return;
		}
	
		//
		// Main loop (Waiting and processing the commands from the message queue)
		//
		String		commandLine			= null;
		Calendar	lastDate			= Calendar.getInstance();
		
		while (true) {
			
			// Wait for next command on the Domino message queue
			setAddinState("Idle");
			commandLine = getCommand();
			setAddinState("Processing command " + commandLine);
			
			switch (commandLine.toLowerCase()) {
				
				// Check if command "Help!" entered
				case "help!": {
					logMessage("Quit!       Terminate the add-in thru the framework");
					logMessage("Debug!      Enable the debug logging to the console");
					logMessage("NoDebug!    Disable the debug logging to the console");
					logMessage("Heartbeat!  Manually start heartbeat processing (automatically done every 15 seconds)");
					logMessage("Help!       Displays this help text");
					continue;
				}

				// Check if add-in shutdown needed (Quit, Exit, Domino server shutdown or error)
				case "quit!": {

					setAddinState("Termination in progress");
				
					logDebug(JADDIN_NAME + " termination in progress");
				
					// Call the user addInStop() method
					if (isJAddinThreadAlive()) {
						try {
							logDebug("Calling " + this.userAddinName + ".addinStop()");
							classAddinStop.invoke(this.jAddinThread, new Object[] {});
							waitMilliSeconds(1000L);
						} catch (Exception e) {
							logMessage("Unhandled exception in " + this.userAddinName + ".addinStop(): " + e.toString());
							jAddinCleanup();
							break;
						}
					}
				
					// Try to stop the thread thru interrupt
					if (isJAddinThreadAlive()) {
						logDebug("Sending interrupt to " + this.userAddinName);
						this.jAddinThread.interrupt();
						waitMilliSeconds(250L);
						
						// Wait 5 seconds for thread termination
						logDebug("Waiting for " + this.userAddinName + " termination");
						
						for (int index = 0; index < 20; index++) {
		
							if (!isJAddinThreadAlive()) {
								logDebug(this.userAddinName + " has terminated");
								break;
							}
									
							waitMilliSeconds(250L);
						}
					}
				
					// The thread did not terminate itself - There is nothing we can do :(
					if (isJAddinThreadAlive()) {
						logMessage("Error: The addin thread " + this.userAddinName + " could not be stopped");
					}
					
					// Terminate the main loop
					jAddinCleanup();
					break;
				}

				// Check if command "Debug!" entered
				case "debug!": {
					setDebugState(true);
					logMessage("Debug logging enabled");
					continue;
				}
			
				// Check if command "NoDebug!" entered
				case "nodebug!": {
					setDebugState(false);
					logMessage("Debug logging disabled");
					continue;
				}
			
				// Check if command "Heartbeat!" entered
				case "heartbeat!": {

					setAddinState("Performing heartbeat processing");
	
					//
					// Check if user thread has terminated
					//
					if (!isJAddinThreadAlive()) {
						logMessage("Abnormal completion of " + this.userAddinName + " detected");
						jAddinCleanup();
						break;
					}
					
					//
					// Check if JVM heap space too small
					//
					checkHeapSpace();
	
					//
					// Check if next hour
					//
					Calendar currentDate = Calendar.getInstance();

					if (currentDate.get(Calendar.HOUR_OF_DAY) != lastDate.get(Calendar.HOUR_OF_DAY)) {
	
						// Call the user addinNextHour() method
						try {
							logDebug("Calling " + this.userAddinName + ".addinNextHour()");
							classAddinNextHour.invoke(this.jAddinThread, new Object[] {});
						} catch (Exception e) {
							logMessage("Unhandled exception in " + this.userAddinName + ".addinNextHour(): " + e.toString());
							// Write the stack trace directly to the standard output
							e.printStackTrace();
							jAddinCleanup();
							break;
						}
					}
					
					//
					// Check if next day
					//
					if (currentDate.get(Calendar.DAY_OF_MONTH) != lastDate.get(Calendar.DAY_OF_MONTH)) {

						// Call the user addinNextDay() method
						try {
							logDebug("Calling " + this.userAddinName + ".addinNextDay()");
							classAddinNextDay.invoke(this.jAddinThread, new Object[] {});
						} catch (Exception e) {
							logMessage("Unhandled exception in " + this.userAddinName + ".addinNextDay(): " + e.toString());
							// Write the stack trace directly to the standard output
							e.printStackTrace();
							jAddinCleanup();
							break;
						}
					}
		
					lastDate = currentDate;
					
					// Wait for next command from queue
					continue;
				}

				default: {

					// Call the user method addinCommand(command) to process the command
					try {
						logDebug("Calling " + this.userAddinName + ".addinCommand()");
						classAddinCommand.invoke(this.jAddinThread, new Object[] {new String(commandLine)});
					} catch (Exception e) {
						logMessage("Unhandled exception in " + this.userAddinName + ".addinCommand(): " + e.toString());
						// Write the stack trace directly to the standard output
						e.printStackTrace();
						jAddinCleanup();
						break;
					}

					// Wait for next command from queue
					continue;
				}
			}
			
			// If we came here, there was an error (or quit) and we have to exit the loop
			break;
		}
		
		//
		// Terminate the main thread
		//
		jAddinCleanup();
	}

	/**
	 * Send Quit command to the Domino message queue to signal termination.
	 * 
	 * Note: This method is also called by the JAddinThread and the user add-in
	 */
	public final void sendQuitCommand() {
		logDebug("Sending Quit command to Domino message queue");
		this.dominoMsgQueue.putQuitMsg();
	}
	
	/**
	 * Set the text of the add-in which is shown in command <code>"show tasks"</code>.
	 * 
	 * Note: This method is also called by the JAddinThread and the user add-in
	 * 
	 * @param	id		Domino task id
	 * @param	message	Text to be set
	 */
	public final void setAddinState(int id, String message) {
		
		if (id == 0)
			return;
		
		AddInSetStatusLine(id, message);
	}

	/**
	 * Set the text of the add-in which is shown in command <code>"show tasks"</code>.
	 * 
	 * @param	text	Text to be set
	 */
	private final void setAddinState(String text) {
		
		if (this.dominoTaskID == 0)
			return;
		
		AddInSetStatusLine(this.dominoTaskID, text);
	}
	
	/**
	 * Set the debug flag
	 * 
	 * Note: This method is also called by the JAddinThread and the user add-in
	 * 
	 * @param	debugFlag	Enable or disable the debug logging
	 */
	public final void setDebugState(boolean debugFlag) {
		this.debugState = debugFlag;
	}
	
	/**
	 * Set the server statistic which is shown in command <code>"Show Stat"</code>.
	 * 
	 * Note: This method is also called by the JAddinThread and the user add-in
	 * 
	 * @param addinName		Name of statistics package
	 * @param statsName		Name of statistics
	 * @param value			Statistics value
	 */
	public final void setDominoStatistic(String addinName, String statsName, Double value) {
		StatUpdate(addinName, statsName, JavaServerAddin.ST_UNIQUE, JavaServerAddin.VT_NUMBER, value);
	}
	
	/**
	 * Set the server statistic which is shown in command <code>"Show Stat"</code>.
	 * 
	 * Note: This method is also called by the JAddinThread and the user add-in
	 * 
	 * @param addinName		Name of statistics package
	 * @param statsName		Name of statistics
	 * @param text			Statistics string
	 */
	public final void setDominoStatistic(String addinName, String statsName, String text) {
		StatUpdate(addinName, statsName, JavaServerAddin.ST_UNIQUE, JavaServerAddin.VT_TEXT, text);
	}

	/**
	 * Delay the execution of the current thread.
	 * 
	 * Note: This method is also called by the JAddinThread and the user add-in
	 * 
	 * @param	sleepTime	Delay time in milliseconds
	 */
	public final void waitMilliSeconds(long sleepTime) {
		try {
			Thread.sleep(sleepTime);
		} catch (Exception e) {
			logDebug("Method waitMilliSeconds(" + sleepTime + ") interrupted: " + e.toString());
			Thread.currentThread().interrupt();
		}
	}
}
