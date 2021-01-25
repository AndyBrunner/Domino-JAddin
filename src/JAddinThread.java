import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Vector;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import lotus.domino.NotesFactory;
import lotus.domino.NotesException;
import lotus.domino.NotesThread;
import lotus.domino.Session;
import lotus.domino.Stream;
import lotus.domino.Base;
import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.MIMEEntity;
import lotus.domino.View;
import lotus.domino.ViewEntry;
import lotus.domino.ViewEntryCollection;

/**
 * This abstract class must be implemented by the user add-in. JAddinThread is started as an separate thread by
 * JAddin. It establishes the JAddin user framework and calls the user code. The code running in this thread
 * should avoid any long-running or blocking code to prohibit delays in processing of the IBM Domino message queue.
 * 
 * @author	andy.brunner@abdata.ch
 * @version	2.1.2 - 2021-01-25
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
	 */
	public JAddinThread() {
	}
	
	/**
	 * Return live state of JAddin main thread .
	 * 
	 * @return	Indicator if JAddin main thread is alive or not
	 */
	public final boolean isJAddinAlive() {
		
		if ((this.jAddin != null) && (this.jAddin.isAlive()))
			return true;

		this.jAddin = null;
		return false;
	}

	/**
	 * This is the first method called by the main JAddin thread to initialize the JAddinThread.
	 * 
	 * @param mainThread	JAddin thread
	 * @param args			Passed arguments or null (<code>"Load RunJava JAddin AddinName xxxxxxxx"</code>)
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
			setDominoStatistic(this.STAT_DOMINO_VERSION, dominoSession.getNotesVersion().trim() + " (" + dominoSession.getPlatform() + ')');
			
			logDebug("Domino version: " + dominoSession.getNotesVersion().trim() + " (" + dominoSession.getPlatform() + ')');
			
		} catch (NotesException e) {
			logMessage("Unable to create Domino session object: " + e.text);
			this.startupError = true;
		}
	}
	
	/**
	 * This is the main entry point for the user add-in. It is called by JAddinThread after all initialization work is done.
	 */
	public abstract void addinStart();

	/**
	 * This method is called from the JAddin framework indirectly thru start(). Its main purpose is to call the
	 * user code thru addinStart().
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
	 */
	public abstract void addinStop();
	
	/**
	 * Terminate the current add-in
	 */
	public final void addinTerminate() {
		
		logDebug("-- addinTerminate()");
		
		// Terminate the thread
		immediateTermination();
	}

	/**
	 * This method is executed for every command entered at the Domino console, e.g. <code>"Tell AddinName xxxxxxxx"</code>.
	 * 
	 * @param	command	Passed command line
	 */
	public void addinCommand(String command) {
		logMessage("This add-in does not support any console commands except 'Quit'");
	}
	
	/**
	 * This method is called at the beginning of every hour.
	 */
	public void addinNextHour() {
	}

	/**
	 * This method is called at the beginning of every new day.
	 */
	public void addinNextDay() {
	}
	
	/**
	 * This method performs all necessary cleanup tasks.
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
	 */
	public final void setAddinState(String message) {
		if (isJAddinAlive())
			jAddin.setAddinState(this.dominoTaskID, message);
	}
	
	/**
	 * Set the debug state
	 * 
	 * @param	debugState	Debug state 
	 */
	public final void setDebugState(boolean debugState) {
		if (isJAddinAlive())
			jAddin.setDebugState(debugState);
	}

	/**
	 * Get the debug state
	 * 
	 * @return	Status of debug logging
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
	 */
	public final void setDominoStatistic(String statsName, Double value) {
		if (isJAddinAlive())
			jAddin.setDominoStatistic(this.userAddinName, statsName, value);
	}
	
	/**
	 * Delete the Domino statistics show in response to 'Show Stat' command.
	 * 
	 * @param statsName		Name of statistics
	 */
	public final void deleteDominoStatistic(String statsName) {
		if (isJAddinAlive())
			jAddin.deleteDominoStatistic(this.userAddinName, statsName);
	}
	
	/**
	 * Get the parameters passed to the add-in.
	 * 
	 * @return	Arguments passed to this add-in or null
	 */
	public final String getAddinParameters() {
		return this.startArguments;
	}
	
	/**
	 * Write a log message to the Domino console. The message string will be prepended with the add-in name
	 * followed by a column, e.g. <code>"HelloWorld: xxxxxxxx"</code>
	 * 
	 * @param 	message		Message to be displayed
	 */
	public final void logMessage(String message) {
		if (isJAddinAlive())
			jAddin.logMessage(this.userAddinName, message);
	}
	
	/**
 	 * Write debug message to the Domino console. The message string will be prepended with the debugging
	 * information, e.g. <code>"DEBUG: AddinName.MethodName(LineNumber) xxxxxxxx"</code>.
	 * 
	 * @param	message		Message to be displayed
	 */
	public final void logDebug(String message) {
		
		if (isJAddinAlive() && jAddin.getDebugState()) {
			jAddin.logDebug(this.userAddinName, message);
		}
	}
	
	/**
	 * Delay the execution of the thread
	 * 
	 * @param	waitTime	Wait time in milliseconds
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
	 * Encode passed buffer to padded Base64 string
	 * 
	 * @param buffer	Buffer to convert
	 * @return Base64 encoded string in upper-case
	 */
	public final synchronized String toBase64(byte[] buffer) {
		
		try {
			return (Base64.getEncoder().encodeToString(buffer));
		} catch (Exception e) {
			logMessage("Unable to encode buffer to Base64: " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Decode passed padded Base64 string
	 * 
	 * @param data	Base64 string to decode
	 * @return Decoded buffer
	 */
	public final synchronized byte[] fromBase64(String data) {
		
		try {
			return (Base64.getDecoder().decode(data));
		} catch (Exception e) {
			logMessage("Unable to decode from Base64 string: " + e.getMessage());
			return (new byte[0]);
		}
	}
	
	/**
	 * AES-128 encrypt the passed buffer with the passed secret key.
	 * 
	 * @param dataBuffer Clear text buffer
	 * @param secretKey	Secret key for encryption
	 * @return Encrypted buffer
	 */
	public final synchronized byte[] encryptAES(byte[] dataBuffer, byte[] secretKey) {
				
		try {
			// MD5 hash the secret key and trim down hash to 128 bits
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(secretKey);
			byte[] secretKeyHash128Bit = Arrays.copyOf(messageDigest.digest(), 16);
			SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyHash128Bit, "AES");
			
			// Create the cipher
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

			// Encrypt the buffer
			return (cipher.doFinal(dataBuffer));
			
		} catch (Exception e) {
			logMessage("Unable to encrypt to AES-128 buffer: " + e.getMessage());
			return (new byte[0]);
		}
	}
	
	/**
	 * AES-128 decrypt the passed buffer with the passed secret key.
	 * 
	 * @param dataBuffer Clear text buffer
	 * @param secretKey	Secret key for encryption
	 * @return Encrypted buffer
	 */
	public final synchronized byte[] decryptAES(byte[] dataBuffer, byte[] secretKey) {

		try {
			
			// MD5 hash the secret key and trim down hash to 128 bits
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(secretKey);
			byte[] secretKeyHash128Bit = Arrays.copyOf(messageDigest.digest(), 16);
			SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyHash128Bit, "AES");
			
			// Create the cipher
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

			// Encrypt the buffer
			return (cipher.doFinal(dataBuffer));
			
		} catch (Exception e) {
			logMessage("Unable to decrypt from AES-128 buffer: " + e.getMessage());
			return (new byte[0]);
		}
	}
	
	/**
	 * Hash the passed byte array
	 * @param hashType	Hash type (MD5, SHA-1, SHA-256)
	 * @param buffer	Buffer to hash
	 * @return	Hash code or empty byte array for errors
	 */
	public final synchronized byte[] generateHash(String hashType, byte[] buffer) {
		
		try {
			MessageDigest messageDigest = MessageDigest.getInstance(hashType);
			messageDigest.update(buffer);
			return ( messageDigest.digest());
		} catch (Exception e) {
			logMessage("Unable to hash with " + hashType + " algorithm: " + e.getMessage());
			return (new byte[0]);
		}
	}

	/**
	 * Return last error message from the dbXXXX methods.
	 *
	 * @return String	Error message
	 */
	public final String dbGetLastErrorMessage() {
		return (this.dbLastErrorMessage);
	}
	
	/**
	 * Recycle Domino object(s).
	 * 
	 * @param	dominoObjects... Domino object(s) to be recycled (supports arrays and Vectors)
	 * @return	Indicator (Success or failure)
	 */
	public final boolean dbRecycleObjects(Object... dominoObjects) {
	
		// Initialize
		this.dbLastErrorMessage = null;

		boolean returnFlag = true;
		
		// Call the recycle() method for each passed Domino object to free the allocated non-Java memory
		for (Object object : dominoObjects) {
			
			if (object != null) {
				
				// Recursively call this method for arrays or vectors
				if (object.getClass().isArray()) {
					Object[] objectArray = (Object[]) object;

					for (Object innerObject : objectArray)
						dbRecycleObjects(innerObject);
				}
				
				if (object instanceof Vector) {
					Vector<?> objectVector = (Vector<?>) object;
					for (Object innerObject : objectVector)
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
	 * Return the Domino session object.
	 * 
	 * @return	Session		Domino session object
	 */
	public final lotus.domino.Session dbGetSession() {
		return this.dominoSession;
	}

	/**
	 * Open the Domino database.
	 * 
	 * @param	dbName	Database name with path
	 * @return	Domino database object (must use <code>dbRecycleObject()</code> or null for errors 
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
			if (db == null)
				return null;

			if (!db.isOpen()) {
				dbRecycleObjects(db);
				return null;
			}
		} catch (Exception e) {
			logDebug("Domino database " + dbName + " open failed: " + e.getMessage());
			this.dbLastErrorMessage = e.getMessage();
			dbRecycleObjects(db);
			return null;
		}
		
		// Return database object
		return (db);
	}
	
	/**
	 * Check if Domino database is open.
	 * 
	 * @param	db		Domino database
	 * @return	True if database is open, false otherwise
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
	 * Get all documents or documents matching a key from a view.
	 * 
	 * @param	db					Domino database
	 * @param	viewName			Domino view name
	 * @param	key					Key for lookup or null to return all documents
	 * @return	Documents (must use <code>dbRecycleObjects()</code> or null if error or empty
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
				
				dbRecycleObjects(dominoViewEntryCollection, dominoView);
				return documentVector;
			}
			
			logDebug("View " + dominoDbName + '/'+ viewName + " entries: " + dominoViewEntryCollection.getCount());
			
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
	 * Get a single Domino document based on the passed key.
	 * 
	 * @param	db			Domino database
	 * @param	viewName	Domino view name
	 * @param	key			Lookup key 
	 * @return	Domino document (must use <code>dbRecylceObjects()</code>) or null if error
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
	 * Get item in Domino document.
	 * 
	 * @param	document	Domino document
	 * @param	itemName	Domino item name
	 * @return	Item value converted to string or null if error
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
	 * Set item in Domino document.
	 * 
	 * @param	document	Domino document
	 * @param	itemName	Domino item name
	 * @param	data		Item to be set
	 * @return	Success or error
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
	 * Save the Domino document.
	 * 
	 * @param	document	Domino document
	 * @return	Indicator (success or failure)
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
	 * @param	from		Senders name
	 * @param	to			Recipient name
	 * @param	subject		Subject
	 * @param	contentType	Content type of body, e.g. "text/html"
	 * @param	body		Body data
	 * @return	Success or failure indicator 			 
	 */
	public final boolean dbSendMessage(String principal, String from, String to, String subject, String contentType, byte[] body) {
		return (dbSendMessage(principal, from, null, to, null, null, subject, contentType, body));
	}
		
	/**
	 * Create and send a message. If the message delivery fails, a message will be written to the Domino console.
	 * 
	 * @param	principal	Principal name or null
	 * @param	from		Senders name
	 * @param	replyTo		Reply address or null
	 * @param	to			Recipient name
	 * @param	cc			Copy recipient or null
	 * @param	bcc			Blind carbon copy recipient or null
	 * @param	subject		Subject
	 * @param	contentType	Content type of body, e.g. "text/html"
	 * @param	body		Body data
	 * @return	Success or failure indicator 			 
	 */
	public final boolean dbSendMessage(String principal, String from, String replyTo, String to, String cc, String bcc, String subject, String contentType, byte[] body) {
		
		// Initialize
		this.dbLastErrorMessage = null;

		// Check arguments and set defaults
		if ((principal != null) && (principal.length() == 0))
			principal = null;
		
		if ((from == null) || (from.length() == 0)) 
			return false;
		
		if ((to == null) || (to.length() == 0)) 
			return false;

		if ((cc != null) && (cc.length() == 0))
			cc = null;

		if ((bcc != null) && (bcc.length() == 0))
			bcc = null;
		
		if ((subject != null) && (subject.length() == 0))
			subject = "(No subject)";
		
		if ((contentType == null) || (contentType.length() == 0))
			contentType = "Text/Plain";

		if ((body == null) || (body.length == 0)) {
			body = "(No content)".getBytes();
		}
		
		// Variables
		Stream			dominoStream		= null;
		Database		dominoMailBox		= null;
		Document		mailDocument		= null;
		MIMEEntity		dominoMIMEEntity	= null;
		DateTime		dominoDateTime		= null;
	
		logDebug("-- dbSendMessage()");
		
		// Open router mail box mail.box or mail1.box
		dominoMailBox = dbOpen("mail.box");
		
		if (dominoMailBox == null) {
			dominoMailBox = dbOpen("mail1.box");
			
			if (dominoMailBox == null) {
				logMessage("Unable to open Domino router mail box");
				return false;
			}
		}

		logDebug("Sending message: From " + from + " to " + to);
		
		boolean dominoMIMEState = false; 
				
		try {
			// Disable MIME conversion
			dominoMIMEState = dbGetSession().isConvertMime();
			dbGetSession().setConvertMime(false);
			
			// Get Domino domain name
			String dominoDomain = dbGetSession().getEnvironmentString("Domain", true);
			
			// Create mail message
			mailDocument = dominoMailBox.createDocument();
			
			// Set required fields
			mailDocument.replaceItemValue("Form", "Memo");
			
			if (principal != null)
				mailDocument.replaceItemValue("Principal", '\"' + principal + "\"<" + from + ">@" + dominoDomain);
			else
				mailDocument.replaceItemValue("Principal", from + '@' + dominoDomain);

			mailDocument.replaceItemValue("From", from);
			mailDocument.replaceItemValue("INETFrom", from);
			mailDocument.replaceItemValue("Sender", from);
			mailDocument.replaceItemValue("SMTPOriginator", from);
			mailDocument.replaceItemValue("SendTo", to + '@' + dominoDomain);
			mailDocument.replaceItemValue("Recipients", to + '@' + dominoDomain);
			
			dominoDateTime = dbGetSession().createDateTime("Today");
		    dominoDateTime.setNow();
			mailDocument.replaceItemValue("PostedDate", dominoDateTime);
	
			if (replyTo != null)
				mailDocument.replaceItemValue("ReplyTo", replyTo);
			
			if (cc != null)
				mailDocument.replaceItemValue("CopyTo", cc);
			
			if (bcc != null)
				mailDocument.replaceItemValue("BlindCopyTo", bcc);
	
			mailDocument.replaceItemValue("Subject", subject);

			// Set MIME body of message
			dominoStream = dbGetSession().createStream();
			dominoStream.write(body);			
			dominoMIMEEntity = mailDocument.createMIMEEntity("Body");
			dominoMIMEEntity.setContentFromBytes(dominoStream, contentType, MIMEEntity.ENC_NONE);
			dominoStream.truncate();
			dominoStream.close();
			
			// Store the document in the router mail box for further delivery
			mailDocument.save(true);

			// Reset MIME conversion state
			dbGetSession().setConvertMime(dominoMIMEState);
			
			logDebug("Document successfully created in router mail box for " + to);
			dbRecycleObjects(dominoDateTime, dominoMIMEEntity, dominoStream, mailDocument, dominoMailBox);
			return true;
			
		} catch (NotesException e) {
			logMessage("Unable to create mail document in router mail box: " + e.text);
			this.dbLastErrorMessage = e.getMessage();
			dbRecycleObjects(dominoDateTime, dominoMIMEEntity, dominoStream, mailDocument, dominoMailBox);
			return false;
		}
	}

	/**
	 * Immediately terminate the thread.
	 */
	@SuppressWarnings("deprecation")
	private void immediateTermination() {

		// Cleanup the resources
		addinCleanup();
		
		// Terminate the thread (Not nice, but I don't know of a better way)
		this.stop();
	}
	
	/**
	 * Finalize is called by the JVM when this object is removed from memory.
	 */
	public void finalize() {
		addinCleanup();
		super.finalize();
	}
}
