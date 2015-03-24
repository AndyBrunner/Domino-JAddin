import lotus.domino.NotesFactory;
import lotus.domino.NotesException;
import lotus.domino.NotesThread;
import lotus.domino.Session;
import lotus.domino.Base;

/**
 *	This abstract class is started as an separate thread by the JAddin. It establishes the JAddin user framework and calls the
 *	user code.
 *
 *	@see	#addinStart()
 *	@see	#addinStop()
 *	@see	#addinCommand(String)
 */
public abstract class JAddinThread extends NotesThread {
	
	// Instance variables
	private JAddin					xJAddin			= null;
	private String					xStartArguments	= null;
	private String					xUserAddinName	= null;
	private Session					xDominoSession	= null;
	private	boolean					xMustExit		= false;
	private	boolean					xCleanupDone	= false;

	/** Dummy constructor
	 * 
	 */
	public JAddinThread() {
	}
	
	/** This method is called from the JAddin framework. Its main purpose is to call the user code thru addinStart().
	 * 
	 */
	public final void runNotes() {
		
		logDebug("Method runNotes() called");
		
		// Check if addinInitialize() has failed
		if (xMustExit) {
			addinCleanup();
			return;			
		}
		
		// Set the addin task status
		setAddinState(null);
		
		// Call the user main entry point
		try {
			logDebug("Calling " + xUserAddinName + ".addinStart()");
			addinStart();
			logDebug("Returning from " + xUserAddinName + ".addinStart()");
		} catch (Exception e) {
			// TODO
//			logMessage("ERROR - Unhandled exception in addinStart(): " + e.getMessage());

			if (xJAddin.getDebugState())
				e.printStackTrace();
		}
				
		addinCleanup();
	}

	/** This method is called by the main JAddin thread to initialize the JAddinThread
	 * 
	 * @param pMainThread		JAddin thread
	 * @param pStartArguments	Passed arguments or null ("Load RunJava JAddin AddinName xxxxx")
	 */
	public final void addinInitialize(JAddin pMainThread, String pStartArguments) {
		
		// Save the passed arguments
		this.xJAddin			= pMainThread;
		this.xStartArguments	= pStartArguments;
		
		// Get name of the addin Java class
		xUserAddinName = this.getClass().getName();

		// Set the threads name (default is "Thread-x")
		setName(xUserAddinName);
		
		logDebug("Method addinInitialize() called");
		
		// Create Domino session
		try {
			logDebug("Creating the Domino session");
			xDominoSession = NotesFactory.createSession();
		} catch (NotesException e) {
			logMessage("ERROR - Unable to create the Domino session object: " + e.text);
			xMustExit = true;
		}
	}
	
	/**
	 * This method is called by JAddinThread after all initialization work is done. When this method returns,
	 * all necessary cleanup tasks are executed and the addin will terminate.
	 */
	public abstract void addinStart();

	/**
	 * This method is executed when the command "Quit" or "Exit" is entered or during Domino server shutdown.  After this method returns, 
	 * the addin will be terminated.
	 * 
	 * Note: This method is called asynchronously from the JAddin framework and not from the users thread. It should return as quickly as
	 * possible to avoid any processing delays of the Domino server message queue.
	 */
	public abstract void addinStop();
	
	/**
	 * This method is executed for every command entered at the Domino console, e.g. <code>"Tell AddinName xxxxxxxx"</code>.
	 * 
	 * Note: This method is called asynchronously from the JAddin framework and not from the users thread. It should return as quickly as
	 * possible to avoid any processing delays of the Domino server message queue.
	 * 
	 * @param 	pCommand	Command line
	 */
	public void addinCommand(String pCommand) {
		logDebug("Method addinCommand() not implemented by user thread");
		logMessage("This addin does not support console commands except 'Quit'");
	}
	
	/**
	 * This method is called at the beginning of an hour.
	 * 
	 * Note: This method is called asynchronously from the JAddin framework and not from the users thread. It should return as quickly as
	 * possible to avoid any processing delays of the Domino server message queue.
	 */
	public void addinNextHour() {
		logDebug("Method addinNextHour() not implemented by user class");
	}

	/**
	 * This method is called at the beginning of a new day.
	 * 
	 * Note: This method is called asynchronously from the JAddin framework and not from the users thread. It should return as quickly as
	 * possible to avoid any processing delays of the Domino server message queue.
	 */
	public void addinNextDay() {
		logDebug("Method addinNextDay() not implemented by user class");
	}
	
	/**
	 * Terminate the current addin.
	 * 
	 * @deprecated Since 1.2.1
	 */
	@Deprecated
	public final void addinTerminate() {
		logDebug("Warning: The deprecated method addinTerminate() should no longer be used");
	}

	/**
	 * Set the addin status message text. This text is shown in response to the Domino console command <code>"show tasks"</code>.
	 * 
	 * @param 	pMessage	Status message or null to set the string to "Idle"
	 */
	public final void setAddinState(String pMessage) {
		
		if (isJAddinAlive())
			xJAddin.setAddinState(pMessage);
	}
	
	/**
	 * Set the debug state.
	 * 
	 * @param	pDebugState Debug state 
	 */
	public final void setDebugState(boolean pDebugState) {
	
		if (isJAddinAlive())
			xJAddin.setDebugState(pDebugState);
	}

	/**
	 * Get the parameters passed to the addin.
	 * 
	 * @return String with the arguments or null
	 */
	public final String getAddinParameters() {
		return xStartArguments;
	}
	
	/**
	 * Return the Domino session object.
	 * 
	 * @return Domino object
	 */
	public final lotus.domino.Session getDominoSession() {
		return xDominoSession;
	}

	/**
	 * Write a log message to the Domino console. The message string will be prepended with the addin name followed by a column,
	 * e.g. <code>"HelloWorld: xxxxxxxx"</code>
	 * 
	 * @param 	pMessage	Message to be displayed
	 */
	public final void logMessage(String pMessage) {

		if (isJAddinAlive())
			xJAddin.logMessage(xUserAddinName, pMessage);
	}
	
	/**
	 * Write debug message to the standard output (Domino console). The message string will be prepended by Domino
	 * and by this method with e.g. <code>"RunJava JVM: AddInName(ThreadID).MethodName(LineNumber): xxxxxxxx"</code>.
	 * The message is only logged if the debug flag is enabled thru <code>setDebug(true)</code> or thru the Domino console
	 * command "Tell AddinName Debug!".
	 * 
	 * @param	pMessage	Message to be displayed
	 * @see		#setDebugState(boolean)			 
	 */
	public final void logDebug(String pMessage) {
		
		if (isJAddinAlive()) {
			if (xJAddin.getDebugState()) {
				// Format thread id
				String classMethod = "" + '[' + Thread.currentThread().getId() + ']';
		    
				// Get calling method name
				StackTraceElement stackTraceElements[] = Thread.currentThread().getStackTrace();
				    
				if (stackTraceElements.length > 3)
					classMethod += '.' + stackTraceElements[3].getMethodName() + '(' + stackTraceElements[3].getLineNumber() + ')';

				System.out.println(xUserAddinName + classMethod + ": " + pMessage);
			}
		}
	}
	
	/**
	 * Create and send a message. If the message delivery fails, a message will be written to the Domino console.
	 * 
	 * @param	pMessageFrom	Senders name
	 * @param	pMessageTo Recipient name
	 * @param	pMessageSubject Subject of message
	 * @param	pMessageBody Body of message
	 * 
	 * @return	Success or failure indicator 			 
	 */
	public final boolean sendMessage(String pMessageFrom, String pMessageTo, String pMessageSubject, String pMessageBody) throws Exception {
		return sendMessage(pMessageFrom, pMessageTo, null, null, pMessageSubject, pMessageBody);
	}
	
	/**
	 * Create and send a message. If the message delivery fails, a message will be written to the Domino console.
	 * 
	 * @param	pMessageFrom	Senders name
	 * @param	pMessageTo Recipient name
	 * @param	pMessageCC Carbon copy recipient or null
	 * @param	pMessageBCC Blind carbon copy recipient or null
	 * @param	pMessageSubject Subject of message
	 * @param	pMessageBody Body of message
	 * 
	 * @return	Success or failure indicator 			 
	 */
	public final boolean sendMessage(String pMessageFrom, String pMessageTo, String pMessageCC, String pMessageBCC, String pMessageSubject, String pMessageBody) throws Exception {
		
		// Variables
		lotus.domino.Database		notesDatabase	= null;
		lotus.domino.Document		notesDocument	= null;
		lotus.domino.RichTextItem	notesRTItem		= null;
		lotus.domino.DateTime		notesDateTime	= null;

		logDebug("Method sendMessage() called");
		
		try {
			// Open the routers mailbox
			notesDatabase = getDominoSession().getDatabase(null, "mail.box");
			
			if (!notesDatabase.isOpen())
				throw new Exception("Unable to open the Domino database mail.box");
			
			// Create mail message
			notesDocument = notesDatabase.createDocument();
			
			notesDocument.replaceItemValue("Form", "Memo");
			
			if (pMessageFrom != null) {
				notesDocument.replaceItemValue("Principal", pMessageFrom + '@' + getDominoSession().getEnvironmentString("Domain", true));
				notesDocument.replaceItemValue("ReplyTo", pMessageFrom);
			}
					
			notesDocument.replaceItemValue("SendTo", pMessageTo);
			
			if (pMessageCC != null)
				notesDocument.replaceItemValue("CopyTo", pMessageCC);
			
			if (pMessageBCC != null)
				notesDocument.replaceItemValue("BlindCopyTo", pMessageBCC);

			notesDocument.replaceItemValue("Subject", pMessageSubject);
			
			// Set current date and time in field <PostedDate>
			notesDateTime = getDominoSession().createDateTime("Today");
			notesDateTime.setNow();
			notesDocument.replaceItemValue("PostedDate", notesDateTime);

			notesRTItem = notesDocument.createRichTextItem("Body");
			notesRTItem.appendText(pMessageBody);
						
			notesDocument.save(true, false);
			
		} catch (NotesException e) {
			logMessage("ERROR - Unable to send the message: " + e.text);
			return false;
		} finally {
			// Free the Domino objects
			recycleObjects(notesDateTime, notesRTItem, notesDocument, notesDatabase);
			notesDateTime	= null;
			notesRTItem		= null;
			notesDocument	= null;
			notesDatabase	= null;			
		}
		
		logDebug("Message successfully created in <mail.box>");
		
		// Return success to the caller
		return true;
	}
			
	/**
	 * Wait some time.
	 * 
	 * @param	pWaitTime Wait time in milliseconds (0.00x seconds) 
	 */
	public final void waitMilliSeconds(long pWaitTime) {
		try {
			Thread.sleep(pWaitTime);
		} catch (Exception e) {
			//TODO
//			logMessage("ERROR - Unable to delay the thread for " + pWaitTime + " ms: " + e.getMessage());
		}
	}
	
	/**
	 * Wait some time.
	 * 
	 * @param	pWaitTime Wait time in seconds 
	 */
	public final void waitSeconds(int pWaitTime) {
		waitMilliSeconds((long) pWaitTime * 1000L);
	}
	
	/**
	 * Recyle (free) Domino object(s)
	 * 
	 * @param	xObjects... Domino object(s) to be recycled
	 */
	public void recycleObjects(Object... xObjects) {
	
		// Call the recycle() method for each passed Domino object
		for (Object xObject : xObjects) {
			if (xObject != null) {
				if (xObject instanceof Base) {
					try {
						((Base) xObject).recycle();
						} catch (NotesException e) {
							logMessage("ERROR - Unable to recycle Domino object: " + e.text);
					}
				}
			}
		}
	}
	
	/**
	 * Return state of JAddin
	 * 
	 * @return 	Indicator
	 */
	final private boolean isJAddinAlive() {
		
		if (xJAddin == null)
			return false;
		
		if (!xJAddin.isAlive())
		{
			xJAddin = null;
			return false;
		}
		
		return true;
	}

	/**
	 * Performs all necessary cleanup tasks. 
	 */
	private final void addinCleanup() {
		
		logDebug("Method addinCleanup() called");
		
		// Check if cleanup already done
		if (xCleanupDone)
			return;
				
		// Signal termination to the main thread
		if (isJAddinAlive()) {
			logDebug("Signaling termination to the main thread");
			xJAddin.sendQuitCommand();
		}
		
		logDebug("Freeing the Domino resources");
			
		// Free session object
		recycleObjects(xDominoSession);
		xDominoSession = null;
		
		xCleanupDone = true;
	}
	
}
