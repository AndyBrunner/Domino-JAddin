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
 * Notes: 	The code running in this main thread should avoid any long-running or blocking code to prohibit
 * 			delays in processing the HCL Domino message queue. Some of the methods in this class are also
 * 			called by the JAddinThread and the user add-in class.
 * 
 * @author	andy.brunner@k43.ch
 * @see		<a href="https://jaddin.k43.ch">Homepage of Domino-JAddin</a>
 */
public final class JAddin extends JavaServerAddin {
	
	// Constants
	static final String		JADDIN_NAME				= "JAddin";
	static final String		JADDIN_VERSION			= "2.2.1";			// Always keep up with jaddin.k43.ch
	static final String		JADDIN_DATE				= "2025-06-21";		// Always keep up with jaddin.k43.ch
	static final String		STAT_OS_VERSION			= "Domino.Platform";
	static final String		STAT_JVM_VERSION		= "JVM.Version";
	
	static final String		STAT_JVM_HEAPDEFINEDKB	= "JVM.HeapLimitKB";
	static final String		STAT_JVM_HEAPUSEDKB		= "JVM.HeapUsedKB";
	static final String		STAT_JADDIN_VERSION		= JADDIN_NAME + ".VersionNumber";
	static final String		STAT_JADDIN_DATE		= JADDIN_NAME + ".VersionDate";
	static final String		STAT_JADDIN_STARTTIME	= JADDIN_NAME + ".StartedTime";

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
	
	// Instance variables
	private JAddinThread	gUserThread				= null;
	private String[]		gJAddinArgs				= null;
	private	String			gUserAddinName			= null;
	private	String			gUserAddinParameter		= null;
	private MessageQueue	gDominoMsgQueue			= null;
	private int 			gDominoTaskID			= 0;
		
	private boolean			gCleanupDone			= false;
	private boolean			gStartupError			= false;
	private boolean			gDebugState				= false;

	/**
	 * This constructor is called by the HCL Domino RunJava task if no arguments are specified.
	 * (<code>"Load RunJava JAddin"</code>).
	 */
	public JAddin() {
		logMessage("Usage: 'Load RunJava JAddin <AddinName> [AddinParameters]'");
		gStartupError = true;
	}

	/**
	 * This constructor is called by the HCL Domino RunJava task if any command line arguments is specified.
	 * (<code>"Load RunJava JAddin HelloWorld"</code>).
	 * 
	 * @param 	args[]	Passed arguments in the command line
	 */
	public JAddin(String[] args) {
		gJAddinArgs = args;
	}

	/**
	 * Performs all necessary cleanup tasks
	 */
	private final void addinCleanup() {
		
		// Check if cleanup already done
		if (gCleanupDone) {
			return;
		}
		
		logDebug("Entered addinCleanup()");
		
		// Delete the Domino statistics
		deleteDominoStatistic(gUserAddinName, STAT_OS_VERSION);
		deleteDominoStatistic(gUserAddinName, STAT_JADDIN_VERSION);
		deleteDominoStatistic(gUserAddinName, STAT_JADDIN_DATE);
		deleteDominoStatistic(gUserAddinName, STAT_JVM_VERSION);
		deleteDominoStatistic(gUserAddinName, STAT_JVM_HEAPDEFINEDKB);
		deleteDominoStatistic(gUserAddinName, STAT_JVM_HEAPUSEDKB);
		deleteDominoStatistic(gUserAddinName, STAT_JADDIN_STARTTIME);
		
		// Wait 5 seconds for the user add-in to terminate
		if (!waitForThreadStop(5)) {
			logMessage(gUserAddinName + " could not be stopped");
		}
		
		try {
			logDebug("Freeing the Domino resources");

			// Delete the Domino task status line (Show Tasks)
			deleteAddinStatusLine(gDominoTaskID);
			gDominoTaskID = 0;
			
			// Clear any pending message in Domino message queue and close it
			if (gDominoMsgQueue != null) {

				StringBuffer dummyBuffer = new StringBuffer(1024);
				
				while (gDominoMsgQueue.get(dummyBuffer, 1024, 0, 0) == NOERROR) {
					logDebug("Clearing command from Domino message queue");
					waitMilliSeconds(250L);
				}
			
				gDominoMsgQueue.close(0);
				gDominoMsgQueue = null;	
			}	
		} catch (Exception e) {
			logMessage("Unable to cleanup Domino resources: " + e.toString());
		}
		
		gCleanupDone = true;
	}
	
	/**
	 * Check JVM heap space
	 */
	private void checkHeapSpace() {
	
		Runtime	runtime				= Runtime.getRuntime();
		long	memoryMax			= runtime.maxMemory();
		long	memoryUsed			= runtime.totalMemory() - runtime.freeMemory();
		long	memoryUsedPercent	= Math.round((memoryUsed * 100.0) / memoryMax);
		
		// Update statistics
		setDominoStatistic(gUserAddinName, STAT_JVM_HEAPUSEDKB, (double) Math.round(memoryUsed / 1024));

		// Write out warning message if JVM heap space is used more than 90%
		if (memoryUsedPercent > 90) {
			logMessage("Warning: Java VM heap space is " + memoryUsedPercent + "%% used. Consider allocating more memory thru Notes.Ini variable 'JavaMaxHeapSize='");
		}
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
		int messageQueueState = gDominoMsgQueue.get(commandLine, 1024, MessageQueue.MQ_WAIT_FOR_MSG, 15000);
		
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
		return gDebugState;
	}
	
	/**
	 * Return the state of the user thread JAddinThread.
	 *
	 * @return	Status indicator (active or inactive)
	 */
	private final boolean isUserThreadAlive() {
		
		if ((gUserThread != null) && gUserThread.isAlive()) {
			return true;
		} else {
			gUserThread = null;
			return false;
		}
	}
	
	/**
	 * Write a debug message to the Domino console. The message string will be prefixed with the add-in name
	 * and the location in the source code issuing the call e.g. <code>"DEBUG: AddinName.MethodName(LineNumber): xxxxxxxx"</code>.
	 * 
	 * @param	message		Message to be displayed
	 */
	private final void logDebug(String message) {

		if (gDebugState) {
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
		
		if (!gDebugState)
			return;
		
		// Get thread name
		StringBuilder moduleInfo = new StringBuilder(addinName)
				.append(": DEBUG: ")
				.append(addinName)
				.append('.');
		
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
		while (moduleInfo.length() < 55) {
			moduleInfo.append(' ');
		}
				
		AddInLogMessageText(moduleInfo.substring(0, 55) + ' ' + message, 0);
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
		if (gStartupError) {
			return;
		}

		// Set the Java thread name to the class name (default would be "Thread-n")
		setName(JADDIN_NAME);
		
		// Extract the user add-in name (1st parameter)
		gUserAddinName = gJAddinArgs[0];

		// Set initial Domino statistics
		setDominoStatistic(gUserAddinName, STAT_OS_VERSION, System.getProperty("os.version", "n/a") + " (" + System.getProperty("os.name", "n/a") + ")");
		setDominoStatistic(gUserAddinName, STAT_JADDIN_VERSION, JADDIN_VERSION);
		setDominoStatistic(gUserAddinName, STAT_JADDIN_DATE, JADDIN_DATE);
		setDominoStatistic(gUserAddinName, STAT_JVM_VERSION, System.getProperty("java.version", "n/a") + " (" + System.getProperty("java.vendor", "n/a") + ")");
		setDominoStatistic(gUserAddinName, STAT_JVM_HEAPDEFINEDKB, ((double) Runtime.getRuntime().maxMemory()) / 1024);
		setDominoStatistic(gUserAddinName, STAT_JVM_HEAPUSEDKB, (double) Math.round((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024d));
		setDominoStatistic(gUserAddinName, STAT_JADDIN_STARTTIME, JAddin.toISODateUTC(new Date()));

		// Check heap space
		checkHeapSpace();
		
		// Create the status line showed in 'Show Task' console command
		gDominoTaskID = createAddinStatusLine(JADDIN_NAME + " Main Task");
		
		// Set the initial state
		setAddinState("Initialization in progress");
		
		// Process special JAddin arguments and extract optional arguments for the user class (2nd thru n-th parameter)
		if (gJAddinArgs.length > 1) {
			
			gUserAddinParameter = "";
			
			for (int index = 1; index < gJAddinArgs.length; index++) {
				
				switch ((gJAddinArgs[index].toLowerCase())) {
					case "debug!": {
						setDebugState(true);
						logMessage("Enter 'Tell " + gUserAddinName + " NoDebug!' to disable debug logging");
						break;
					}
					case "nodebug!": {
						setDebugState(false);
						logMessage("Enter 'Tell " + gUserAddinName + " Debug!' to enable debug logging");
						break;
					}
					default: {
						// Construct parameter line for thread (without "Debug!" parameter)
						gUserAddinParameter += ' ' + gJAddinArgs[index];
						break;
					}
				}
			}
		}
		
		// Cleanup the command line string
		if (gUserAddinParameter != null) {
			gUserAddinParameter = gUserAddinParameter.trim();
			
			if (gUserAddinParameter.isEmpty()) {
				gUserAddinParameter = null;
			}
		}
	
		logDebug(JADDIN_NAME + " framework version " + JADDIN_VERSION + " / " + JADDIN_DATE);
		logDebug("OS platform: " + System.getProperty("os.version", "n/a") + " (" + System.getProperty("os.name", "n/a") + ")");
		logDebug("JVM version: " + System.getProperty("java.version", "n/a") + " (" + System.getProperty("java.vendor", "n/a") + ")");
		logDebug(gUserAddinName + " will be called with parameter: " + gUserAddinParameter);
		
		// Create and open the message queue
		logDebug("Creating and opening the Domino message queue");

		String messageQueueName	= MSG_Q_PREFIX + gUserAddinName.toUpperCase();
		gDominoMsgQueue = new MessageQueue();
		int messageQueueState = gDominoMsgQueue.create(messageQueueName, 0, 0);
		
		if (messageQueueState == MessageQueue.ERR_DUPLICATE_MQ) {
			logMessage(gUserAddinName + " task is already running");
			addinCleanup();
			return;
		}
		
		if (messageQueueState != MessageQueue.NOERROR) {
			logMessage("Unable to create the Domino message queue");
			addinCleanup();
			return;
		}
		
		if (gDominoMsgQueue.open(messageQueueName, 0) != NOERROR) {
			logMessage("Unable to open Domino message queue");
			addinCleanup();
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
			logDebug("Loading Java class " + gUserAddinName);

			classClass				= Class.forName(gUserAddinName);
			gUserThread		= (JAddinThread) classClass.newInstance();
			classAddinInitialize	= classClass.getMethod("addinInitialize", new Class[] {JAddin.class, String.class});
			classStart				= classClass.getMethod("start", new Class[] {});
			classAddinStop			= classClass.getMethod("addinStop", new Class[] {});
			classAddinCommand		= classClass.getMethod("addinCommand", new Class[] {String.class});
			classAddinNextHour		= classClass.getMethod("addinNextHour", new Class[] {});
			classAddinNextDay		= classClass.getMethod("addinNextDay", new Class[] {});
		}
		catch (Exception e)
		{
			logMessage("Unable to load Java class " + gUserAddinName);
			logMessage("Make sure add-in is in correct directory and name is written with correct upper-/lowercase");
			addinCleanup();
			return;
		}

		// Call the addInInitialize(this, arguments) method
		try {
			logDebug("Calling " + gUserAddinName + ".addinInitialize()");
			classAddinInitialize.invoke(gUserThread, new Object[] {this, gUserAddinParameter});
		} catch (Exception e) {
			logMessage("Unhandled exception in " + gUserAddinName + ".addinInitialize(): " + e.toString());
			addinCleanup();
			return;
		}
		
		// Call the start() method (part of JavaServerAddin) which will then call runNotes()
		try {
			logDebug("Calling " + gUserAddinName + ".start()");
			classStart.invoke(gUserThread, new Object[] {});
		} catch (Exception e) {
			logMessage("Unhandled exception in " + gUserAddinName + ".start(): " + e.toString());
			addinCleanup();
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
					if (isUserThreadAlive()) {
						try {
							logDebug("Calling " + gUserAddinName + ".addinStop()");
							classAddinStop.invoke(gUserThread, new Object[] {});
						} catch (Exception e) {
							logMessage("Unhandled exception in " + gUserAddinName + ".addinStop(): " + e.toString());
							break;
						}
					}
				
					// Give user thread some time to terminate
					waitForThreadStop(3);
					
					// Try to stop the thread thru interrupt
					if (isUserThreadAlive()) {
						logDebug("Sending interrupt to " + gUserAddinName);
						gUserThread.interrupt();
					}
					
					// Terminate the main loop
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
					if (!isUserThreadAlive()) {
						logMessage("Abnormal completion of " + gUserAddinName + " detected");
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
							logDebug("Calling " + gUserAddinName + ".addinNextHour()");
							classAddinNextHour.invoke(gUserThread, new Object[] {});
						} catch (Exception e) {
							logMessage("Unhandled exception in " + gUserAddinName + ".addinNextHour(): " + e.toString());
							// Write the stack trace directly to the standard output
							e.printStackTrace();
							break;
						}
					}
					
					//
					// Check if next day
					//
					if (currentDate.get(Calendar.DAY_OF_MONTH) != lastDate.get(Calendar.DAY_OF_MONTH)) {

						// Call the user addinNextDay() method
						try {
							logDebug("Calling " + gUserAddinName + ".addinNextDay()");
							classAddinNextDay.invoke(gUserThread, new Object[] {});
						} catch (Exception e) {
							logMessage("Unhandled exception in " + gUserAddinName + ".addinNextDay(): " + e.toString());
							// Write the stack trace directly to the standard output
							e.printStackTrace();
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
						logDebug("Calling " + gUserAddinName + ".addinCommand()");
						classAddinCommand.invoke(gUserThread, new Object[] {new String(commandLine)});
					} catch (Exception e) {
						logMessage("Unhandled exception in " + gUserAddinName + ".addinCommand(): " + e.toString());
						// Write the stack trace directly to the standard output
						e.printStackTrace();
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
		addinCleanup();
	}

	/**
	 * Send Quit command to the Domino message queue to signal termination.
	 * 
	 * Note: This method is also called by the JAddinThread and the user add-in
	 */
	public final void sendQuitCommand() {
		logDebug("Sending Quit command to Domino message queue");
		gDominoMsgQueue.putQuitMsg();
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
		
		if (id == 0) {
			return;
		}
		
		AddInSetStatusLine(id, message);
	}

	/**
	 * Set the text of the add-in which is shown in command <code>"show tasks"</code>.
	 * 
	 * @param	text	Text to be set
	 */
	private final void setAddinState(String text) {
		
		if (gDominoTaskID == 0) {
			return;
		}
		
		AddInSetStatusLine(gDominoTaskID, text);
	}
	
	/**
	 * Set the debug flag
	 * 
	 * Note: This method is also called by the JAddinThread and the user add-in
	 * 
	 * @param	debugFlag	Enable or disable the debug logging
	 */
	public final void setDebugState(boolean debugFlag) {
		gDebugState = debugFlag;
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
	 * Wait for user thread termination.
	 * 
	 * @param 	argTimeoutSec	Maximum number of seconds to wait
	 * @return	True (if thread terminated), false otherwise
	 */
	private boolean waitForThreadStop(int argTimeoutSec) {
		
		// Declarations
		final int DELAY_TIME_MS = 250;
		
		// Check argument
		if (argTimeoutSec < 1) {
			logMessage("Error: waitForThreadStop(): argTimeoutSec must be greater than 0");
			return false;
		}
		
		for (int loopIndex = 0; loopIndex < ((argTimeoutSec * 1000) / DELAY_TIME_MS); loopIndex++) {

			if (!isUserThreadAlive()) {
				logDebug(gUserAddinName + " has terminated");
				return true;
			}
					
			waitMilliSeconds(DELAY_TIME_MS);
		}
		
		return false;
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
