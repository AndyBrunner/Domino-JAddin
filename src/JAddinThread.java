import java.util.Vector;

import lotus.domino.NotesFactory;
import lotus.domino.NotesException;
import lotus.domino.NotesThread;
import lotus.domino.Session;
import lotus.domino.Base;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.View;
import lotus.domino.ViewEntry;
import lotus.domino.ViewEntryCollection;
import lotus.domino.RichTextItem;

/**
 * This abstract class is started as an separate thread by the JAddin. It establishes the JAddin user framework
 * and calls the user code. The code running in this thread should avoid any long-running or blocking code to
 * prohibit delays in processing of the IBM Domino message queue.
 * 
 * @author	andy.brunner@abdata.ch
 * @version	2.0.0 - 16-Jan-2019
 * 
 * @see		<a href="https://jaddin.abdata.ch">Homepage of Domino-JAddin</a>
 */
public abstract class JAddinThread extends NotesThread {
	
	// Constants
	final	String		STAT_DOMINO_VERSION = "Domino.Version";
	
	// Instance variables
	private JAddin		jAddin				= null;
	private String		startArguments		= null;
	private String		userAddinName		= null;
	private Session		dominoSession		= null;
	private int			dominoTaskID		= 0;
	private	boolean		startupError		= false;
	private	boolean		cleanupDone			= false;
	private String		dbLastErrorMessage	= null;

	/** Dummy constructor
	 * 
	 * @param 	-
	 * @return	Object	Thread instance
	 */
	public JAddinThread() {
	}
	
	/**
	 * Return live state of JAddin main thread 
	 * 
	 * @param	-
	 * @returns	boolean		Returns true if the JAddin main thread is alive, else otherwise
	 */
	public final boolean isJAddinAlive() {
		
		if ((this.jAddin != null) && (this.jAddin.isAlive()))
			return true;

		this.jAddin = null;
		return false;
	}

	/**
	 * This is the first method called by the main JAddin thread to initialize the JAddinThread
	 * 
	 * @param JAddin	JAddin thread
	 * @param args		Passed arguments or null ("Load RunJava JAddin <AddinName> xxxxxxxx")
	 */
	public final void addinInitialize(JAddin mainThread, String args) {
		
		// Do some initializations
		this.jAddin			= mainThread;
		this.startArguments	= args;
		this.userAddinName	= this.getClass().getName();

		logDebug("-- addinInitialize()");
		
		// Set the thread name (default would be is "Thread-x")
		setName(this.userAddinName);

		// Create the status line showed in 'Show Task' command
		if (isJAddinAlive())
			this.dominoTaskID = jAddin.createAddinStatusLine(jAddin.JADDIN_NAME + " " + this.userAddinName);
		
		// Set the initial state
		setAddinState("Executing addinInitialize() method");
				
		// Create Domino session
		try {
			logDebug("Creating the Domino session");
			this.dominoSession = NotesFactory.createSession();

			// Set initial Domino statistic
			setDominoStatistic(this.STAT_DOMINO_VERSION, dominoSession.getNotesVersion().trim() + ' ' + dominoSession.getPlatform());
			
		} catch (NotesException e) {
			logMessage("Unable to create session object: " + e.text);
			this.startupError = true;
		}
	}
	
	/**
	 * This method is called by JAddinThread after all initialization work is done.
	 * 
	 * @param	-
	 * @return	-
	 */
	public abstract void addinStart();

	/**
	 * This method is called from the JAddin framework indirectly thru start(). Its main purpose is to call the
	 * user code thru addinStart().
	 * 
	 * @param	-
	 * @return	-
	 */
	public final void runNotes() {
		
		logDebug("-- runNotes()");
		
		// Check if addinInitialize() has failed
		if (this.startupError) {
			addinCleanup();
			return;
		}
		
		// Set the initial state
		setAddinState("Initialization in progress");
		
		// Call the user main method addinStart()
		try {
			logDebug("=> " + this.userAddinName + ".addinStart()");
			addinStart();
			logDebug("<= " + this.userAddinName + ".addinStart()");
		} catch (Exception e) {
			// Write the stack trace directly to the standard output
			e.printStackTrace();
			addinCleanup();
			return;
		}
				
		// Cleanup the resources
		addinCleanup();
		
		// Terminate the thread
		return;
	}

	/**
	 * This method is executed when the command "Quit" or "Exit" is entered or during Domino server shutdown. After this
	 * method returns, the add-in will be terminated.
	 * 
	 * Note: This method is called from the JAddin main thread. It should return as quickly as possible to avoid
	 * 		 any processing delays of the Domino server message queue.
	 * 
	 * @param	-
	 * @return	-
	 */
	public abstract void addinStop();
	
	/**
	 * Terminate the current add-in
	 * 
	 * Note: This method is called by JAddin
	 * 
	 * @param	-
	 * @return	-
	 */
	public final void addinTerminate() {
		
		logDebug("-- addinTerminate()");
		
		// Terminate the thread
		immediateTermination();
	}

	/**
	 * This method is executed for every command entered at the Domino console, e.g. <code>"Tell <AddinName> xxxxxxxx"</code>.
	 * 
	 * Note: This method is called from the JAddin main thread. It should return as quickly as possible to avoid
	 * 		 any processing delays of the Domino server message queue.
	 * 
	 * @param	String		command
	 * @return	-
	 */
	public void addinCommand(String command) {
		logMessage("This add-in does not support any console commands except 'Quit'");
	}
	
	/**
	 * This method is called at the beginning of an hour
	 * 
	 * Note: This method is called from the JAddin main thread. It should return as quickly as possible to avoid
	 * 		 any processing delays of the Domino server message queue.
	 * 
	 * @param	-
	 * @return	-
	 */
	public void addinNextHour() {
	}

	/**
	 * This method is called at the beginning of a new day
	 * 
	 * Note: This method is called from the JAddin main thread. It should return as quickly as possible to avoid
	 * 		 any processing delays of the Domino server message queue.
	 * 
	 * @param	-
	 * @return	-
	 */
	public void addinNextDay() {
	}
	
	/**
	 * Performs all necessary cleanup tasks.
	 * 
	 * @param -
	 * @returns -
	 * 
	 */
	private final void addinCleanup() {
	
		logDebug("-- addinCleanup()");
		
		// Check if cleanup already done
		if (this.cleanupDone)
			return;
				
		// Delete Domino statistics
		if (isJAddinAlive()) {
			deleteDominoStatistic(this.STAT_DOMINO_VERSION);
		}
		
		// Delete the Domino task status line (Show Tasks)
		if (isJAddinAlive()) {
			jAddin.deleteAddinStatusLine(this.dominoTaskID);
		}
		this.dominoTaskID = 0;
	
		// Send Quit command to the Domino message queue to gracefully terminate the main thread
		if (isJAddinAlive())
			jAddin.sendQuitCommand();
		
		// Free the Domino session object
		dbRecycleObjects(this.dominoSession);
		this.dominoSession = null;
		
		this.cleanupDone = true;
	}

	/**
	 * Set the add-in status message text. This text is shown in response to the Domino console command <code>"show tasks"</code>.
	 * 
	 * @param 	message		Status message
	 * @return	-
	 */
	public final void setAddinState(String message) {
		if (isJAddinAlive())
			jAddin.setAddinState(this.dominoTaskID, message);
	}
	
	/**
	 * Set the debug state
	 * 
	 * @param	debugFlag	Debug state 
	 * @return	-
	 */
	public final void setDebugState(boolean debugState) {
		if (isJAddinAlive())
			jAddin.setDebugState(debugState);
	}

	/**
	 * Get the debug state
	 * 
	 * @param	- 
	 * @return	boolean		Status of debug logging
	 */
	public final boolean getDebugState() {
		if (isJAddinAlive())
			return (jAddin.getDebugState());
		else
			return (false);
	}

	/**
	 * Set the Domino statistics show in response to 'Show Stat' command.
	 * 
	 * @param statsName		Name of statistics
	 * @param text			Statistics text
	 * @return -
	 */
	public final void setDominoStatistic(String statsName, String text) {
		if (isJAddinAlive())
			jAddin.setDominoStatistic(this.userAddinName, statsName, text);
	}
	
	/**
	 * Set the Domino statistics show in response to 'Show Stat' command.
	 * 
	 * @param statsName		Name of statistics
	 * @param value			Statistics value
	 * @return -
	 */
	public final void setDominoStatistic(String statsName, Double value) {
		if (isJAddinAlive())
			jAddin.setDominoStatistic(this.userAddinName, statsName, value);
	}
	
	/**
	 * Delete the Domino statistics show in response to 'Show Stat' command.
	 * 
	 * @param statsName		Name of statistics
	 * @return -
	 */
	public final void deleteDominoStatistic(String statsName) {
		if (isJAddinAlive())
			jAddin.deleteDominoStatistic(this.userAddinName, statsName);
	}
	
	/**
	 * Get the parameters passed to the add-in
	 * 
	 * @param	-
	 * @return	String		Arguments passed to this add-in or null
	 */
	public final String getAddinParameters() {
		return this.startArguments;
	}
	
	/**
	 * Write a log message to the Domino console. The message string will be prepended with the add-in name
	 * followed by a column, e.g. <code>"HelloWorld: xxxxxxxx"</code>
	 * 
	 * @param 	message		Message to be displayed
	 * @return	-
	 */
	public final void logMessage(String message) {
		if (isJAddinAlive())
			jAddin.logMessage(this.userAddinName, message);
	}
	
	/**
 	 * Write debug message to the Domino console. The message string will be prepended with the debugging
	 * information, e.g. <code>"DEBUG: AddinName.MethodName(LineNumber) xxxxxxxx"</code>.
	 *
	 * @see		#setDebugState(boolean)			
	 * 
	 * @param	message		Message to be displayed
	 * @return	-
	 */
	public final void logDebug(String message) {
		
		if (isJAddinAlive()) {
			if (jAddin.getDebugState()) {
				jAddin.logDebug(this.userAddinName, message);
			}
		}
	}
	
	/**
	 * Delay the execution of the thread
	 * 
	 * @param	waitTime	Wait time in milliseconds
	 * @return	- 
	 */
	public final void waitMilliSeconds(long waitTime) {
		
		if (isJAddinAlive())
			jAddin.waitMilliSeconds(waitTime);
		else {
			logMessage("Unable to delay execution of thread: Main thread is not running");
			// Terminate the thread
			immediateTermination();
		}
	}
	
	/**
	 * Return last error message from the dbXXXX methods.
	 *
	 * @param -
	 * @return String	Error message
	 */
	public final String dbGetLastErrorMessage() {
		return (this.dbLastErrorMessage);
	}
	
	/**
	 * Recycle Domino object(s)
	 * 
	 * @param	dominoObjects... Domino object(s) to be recycled (supports arrays and Vectors)
	 * @returns	boolean			 Indicator (Success or failure)
	 */
	public final boolean dbRecycleObjects(Object... dominoObjects) {
	
		// Initialize
		this.dbLastErrorMessage = null;

		boolean returnFlag = true;
		
		// Call the recycle() method for each passed Domino object to free the allocated non-Java memory
		for (Object object : dominoObjects) {
			
			if (object != null) {
				
				// Recursively call this method for arrays or vectors
				if ((object.getClass().isArray()) || (object instanceof Vector)) {
					for (Object innerObject : dominoObjects)
						dbRecycleObjects(innerObject);
				}
				
				// Recycle the Domino object
				if (object instanceof Base) {
					try {
						((Base) object).recycle();
					} catch (NotesException e) {
						logMessage("Unable to recycle Domino object: " + e.text);
						this.dbLastErrorMessage = e.text;
						returnFlag = false;
					}
				}
			}
		}
		
		// Return indicator
		return (returnFlag);
	}
	
	/**
	 * Return the Domino session object
	 * 
	 * @param	-
	 * @return	Session		Domino session object
	 */
	public final lotus.domino.Session dbGetSession() {
		return this.dominoSession;
	}

	/**
	 * Open the Domino database
	 * 
	 * IMPORTANT: The Domino database object returned must each be recycled by the calling
	 * 			  routine, e.g. <code>dbRecycleObjects(db);</code>
	 * 
	 * @param	String		Database name with path
	 * @returns	Database	Domino database object or null for errors
	 */
	public final Database dbOpen(String dbName) {
		
		// Initialize
		this.dbLastErrorMessage = null;
		
		// Check argument
		if ((dbName == null) || (dbName.length() == 0))
			return null;
		
		Database db = null;

		try {
			// Open Domino database
			db = dbGetSession().getDatabase(null, dbName);
			
			// Return null if any error
			if (db == null) {
				return null;
			}

			if (!db.isOpen()) {
				dbRecycleObjects(db);
				return null;
			}
		} catch (Exception e) {
			logDebug("Domino database " + dbName + " open failed: " + e.getMessage());
			dbRecycleObjects(db);
			this.dbLastErrorMessage = e.getMessage();
			return null;
		}
		
		// Return database object
		return (db);
	}
	
	/**
	 * Check if Domino database is open
	 * 
	 * @param	db		Domino database
	 * @return	boolean	True if database is open, false otherweie
	 */
	public final boolean isDbOpen(Database db) {
		
		// Initialize
		this.dbLastErrorMessage = null;

		// Check arguments
		if ((db == null) || !(db instanceof Database)) 
			return false;
			
		try {
			if (db.isOpen())
				return true;
			else
				return false;
			
		} catch (Exception e) {
			logDebug("Unable to check for open Domino database: " + e.getMessage());
			this.dbLastErrorMessage = e.getMessage();
			return false;
		}
	}
	
	/**
	 * Get all documents or documents matching a key from a view
	 * 
	 * IMPORTANT: The Domino documents returned in the Vector<Document> must each be recycled
	 * 			  by the calling routine, e.g.
	 * 			  <code>for (Document doc : documentVector); dbRecycleObjects(doc);</code>
	 * 
	 * @param	db					Domino database
	 * @param	viewName			Domino view name
	 * @param	key					Key for lookup or null to return all documents
	 * @return	Vector<Document>	Documents or empty Vector if error or empty
	 */
	public final Vector<Document> dbGetAllDocuments(Database db, String viewName, String key) {
		
		// Initialize
		this.dbLastErrorMessage = null;

		Vector<Document> documentVector	= new Vector<Document>(0, 1);
		
		// Check arguments
		if ((db == null) || !(db instanceof Database)) 
			return documentVector;
		
		if (viewName == null) 
			return documentVector;
				
		// Check if database is open
		if (!isDbOpen(db))
			return documentVector;
		
		View				dominoView					= null;
		ViewEntryCollection	dominoViewEntryCollection	= null;
		ViewEntry			dominoViewEntry				= null;
		ViewEntry			dominoViewEntryNext			= null;
		String				dominoDbName				= null;
		
		try {
			dominoDbName = db.getFilePath();
			dominoView	 = db.getView(viewName);
			
			if (dominoView == null) {
				logDebug("Unable to open view " + dominoDbName + '/' + viewName);
				return documentVector;
			}
			
			// Get all view entries or entries matching a key
			if (key == null)
				dominoViewEntryCollection = dominoView.getAllEntries();
			else
				dominoViewEntryCollection = dominoView.getAllEntriesByKey(key);
			
			if ((dominoViewEntryCollection == null) || (dominoViewEntryCollection.getCount() == 0)) {

				if (key == null)
					logDebug("View " + dominoDbName + '/'+ viewName + " is empty");
				else
					logDebug("View " + dominoDbName + '/' + viewName + " has no documents matching key " + key);
				
				dbRecycleObjects(dominoView);
				return documentVector;
			}
			
			// Read thru all view entries and get document
			dominoViewEntry = dominoViewEntryCollection.getFirstEntry();
			
			while (dominoViewEntry != null) {
				
				documentVector.add(dominoViewEntry.getDocument());
				dominoViewEntryNext = dominoViewEntryCollection.getNextEntry();

				// Recycle previous entry
				dbRecycleObjects(dominoViewEntry);
				dominoViewEntry = dominoViewEntryNext;
			}

			// Recycle temporary Domino objects and return data
			dbRecycleObjects(dominoViewEntry, dominoViewEntryCollection, dominoView);
			return documentVector;
			
		} catch (Exception e) {
			logDebug("Unable to read view " + dominoDbName + '/' + viewName + ": " + e.getMessage());
			dbRecycleObjects(dominoViewEntry, dominoViewEntryCollection, dominoView);
			
			// Recycle already read Domino documents and clear Vector
			for (Document doc : documentVector)
				dbRecycleObjects(doc);

			documentVector.removeAllElements();
			
			this.dbLastErrorMessage = e.getMessage();
			return (documentVector);
		}
	}
	
	/**
	 * Get a single Domino document based on the passed key
	 * 
	 * @param	db			Domino database
	 * @param	viewName	Domino view name
	 * @param	key			Lookup key 
	 * @return	Document	Domino document or null if error
	 */
	public final Document dbGetSingleDocumentByKey(Database db, String viewName, String key) {

		// Initialize
		this.dbLastErrorMessage = null;

		// Check arguments
		if ((db == null) || !(db instanceof Database)) 
			return null;
		
		if (viewName == null) 
			return null;
		
		// Check if database is open
		if (!isDbOpen(db))
			return null;
		
		View		dominoView		= null;
		Document	dominoDocument	= null;
		String		dominoDbName	= null;
		
		try {
			dominoDbName = db.getFilePath();
			dominoView	 = db.getView(viewName);
			
			if (dominoView == null) {
				logDebug("Unable to open view " + dominoDbName + '/' + viewName);
				return null;
			}
			
			// Get the first document matching the key
			dominoDocument = dominoView.getDocumentByKey(key, true);
			
			if (dominoDocument == null) {
				logDebug("View " + dominoDbName + '/' + viewName + " has no documents matching key " + key);
				dbRecycleObjects(dominoView);
				return null;
			}

			dbRecycleObjects(dominoView);
			return dominoDocument;
			
		} catch (Exception e) {
			logDebug("Unable to read view " + dominoDbName + '/' + viewName + ": " + e.getMessage());
			dbRecycleObjects(dominoView);
			this.dbLastErrorMessage = e.getMessage();
			return null;
		}
	}
		
	/**
	 * Get item in Domino document
	 * 
	 * @param	document	Domino document
	 * @param	itemName	Domino item name
	 * @return	String		Item value converted to string or null if error
	 */
	public final String dbGetDocumentItem(Document document, String itemName) {
		
		// Initialize
		this.dbLastErrorMessage = null;

		// Check arguments
		if ((document == null) || !(document instanceof Document)) 
			return null;

		if ((itemName == null) || (itemName.length() == 0)) 
			return null;
		
		Vector<?> itemValue = null;
				
		try {
			itemValue = document.getItemValue(itemName);
			
			// Return null if empty item
			if (itemValue.size() == 0)
				return null;

			// Convert returned item value to string
			return (String.valueOf(itemValue.get(0)));
			
		} catch (Exception e) {
			logDebug("Unable to read document item: " + e.getMessage());
			this.dbLastErrorMessage = e.getMessage();
			return null;
		}
	}
	
	/**
	 * Set item in Domino document
	 * 
	 * @param	document	Domino document
	 * @param	itemName	Domino item name
	 * @param	data		Item to be set
	 * @return	boolean		Success or error
	 */
	public final boolean dbSetDocumentItem(Document document, String itemName, Object data) {

		// Initialize
		this.dbLastErrorMessage = null;

		// Check arguments
		if ((document == null) || !(document instanceof Document)) 
			return false;

		if ((itemName == null) || (itemName.length() == 0)) 
			return false;
				
		try {
			document.replaceItemValue(itemName, data);
		} catch (Exception e) {
			logDebug("Unable to set value in item " + itemName + ": " + e.getMessage());
			this.dbLastErrorMessage = e.getMessage();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Save the Domino document
	 * 
	 * @param	document	Domino document
	 * @return	boolean		Indicator (success or failure)
	 */
	public final boolean dbSaveDocument(Document document) {

		// Initialize
		this.dbLastErrorMessage = null;

		// Check arguments
		if ((document == null) || !(document instanceof Document)) 
			return false;
				
		try {
			return document.save(true);
		} catch (Exception e) {
			logDebug("Unable to save Domino document: " + e.getMessage());
			this.dbLastErrorMessage = e.getMessage();
			return false;
		}
	}
	
	/**
	 * Create and send a message. If the message delivery fails, a message will be written to the Domino console.
	 * 
	 * @param	principal	Principal name or null
	 * @param	replyTo		Reply address or null
	 * @param	from		Senders name
	 * @param	to			Recipient name
	 * @param	cc			Copy recipient(s)
	 * @param	bcc			Blind carbon copy recipient(s) or null
	 * @param	subject		Subject or null
	 * @param	body		Message body (String or RichTextItem)
	 * @return	boolean		Success or failure indicator 			 
	 */
	public final boolean dbSendMessage(String principal, String replyTo, String from, String to, String cc, String bcc, String subject, Object body) {
		
		// Initialize
		this.dbLastErrorMessage = null;

		// Check arguments
		if ((from == null) || (from.length() == 0)) 
			return false;
		
		if ((to == null) || (to.length() == 0)) 
			return false;

		if (body == null) {
			return false;
		}
		
 		if (!(body instanceof String) && !(body instanceof RichTextItem)) {
			return false;
 		}
 		
		// Variables
		Database		routerMailBox	= null;
		Document		mailDocument	= null;
		RichTextItem	rtItem			= null;
	
		logDebug("-- dbSendMessage()");
		
		// Open router mail box
		routerMailBox = dbOpen("mail.box");
		
		if (routerMailBox == null) {
			logMessage("Unable to open Domino router mail box");
			return false;
		}
				
		try {
			// Create mail message
			mailDocument = routerMailBox.createDocument();
			
			mailDocument.replaceItemValue("Form", "Memo");
			
			if (principal != null)
				mailDocument.replaceItemValue("Principal", principal + '@' + dbGetSession().getEnvironmentString("Domain", true));
	
			if (replyTo != null)
				mailDocument.replaceItemValue("ReplyTo", replyTo);
			
			if (cc != null)
				mailDocument.replaceItemValue("CopyTo", cc);
			
			if (bcc != null)
				mailDocument.replaceItemValue("BlindCopyTo", bcc);
	
			if (subject != null)
				mailDocument.replaceItemValue("Subject", subject);
	
			// Set body to string or rich-text item
			if (body instanceof String)
				mailDocument.replaceItemValue("Body", body);
			
			if (body instanceof RichTextItem) {
				rtItem = mailDocument.createRichTextItem("Body");
				rtItem.appendRTItem((RichTextItem) body);
			}
						
			// Store the document in the router mail box for further delivery
			mailDocument.send(to);
			
		} catch (NotesException e) {
			logMessage("Unable to create mail document in Domino router mail box: " + e.text);
			dbRecycleObjects(rtItem, mailDocument, routerMailBox);
			this.dbLastErrorMessage = e.getMessage();
			return false;
		}
	
		// Free the Domino objects
		dbRecycleObjects(rtItem, mailDocument, routerMailBox);
		
		// Return success to the caller
		logDebug("Document successfully created in Domino router mail box");
		return true;
	}

	/**
	 * Immediately terminate the thread
	 * 
	 * @param	-
	 * @return	-
	 */
	@SuppressWarnings("deprecation")
	private void immediateTermination() {

		// Cleanup the resources
		addinCleanup();
		
		// Terminate the thread (Not nice, but I don't know of a better way)
		this.stop();
	}
	
	/**
	 * Finalize is called by the JVM when this object is removed from memory
	 * 
	 * @param -
	 * returns -
	 */
	public void finalize() {
		addinCleanup();
		super.finalize();
	}
}
