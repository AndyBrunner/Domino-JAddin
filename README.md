---
description: Java Toolkit for developers writing HCL Domino Server Add-ins
cover: .gitbook/assets/Picture.jpg
coverY: -12
layout:
  cover:
    visible: true
    size: full
  title:
    visible: true
  description:
    visible: true
  tableOfContents:
    visible: true
  outline:
    visible: false
  pagination:
    visible: true
---

# JAddin for HCL Domino Java Framework (Freeware)

<a href="download/download.md" class="button primary">Free Download</a>

{% hint style="info" %}
Follow development at Mastodon [#DominoJAddin](https://swiss.social/tags/dominojaddin)

This tool is also shown on the [_HCL Domino Marketplace_](https://hclsofy.com/domino)
{% endhint %}

## Introduction

The free and open-source JAddin framework acts as a thin and easy to use layer between the HCL Domino RunJava task and your Java application code. It helps you to create Java server tasks by freeing you to learn all the HCL Domino add-in specifics, such as message queue handling, thread creation, communication with the console, resource cleanup, etc. It is written entirely in Java to support all HCL Domino versions and platforms (HCL Domino 9.0.1 FP8 and above).

### **Code Example**

```java
public class HelloWorld extends JAddinThread {

	// Declarations
	boolean threadRunning = true;
	
	// This is the main entry point. When this method returns, the add-in terminates.
	public void addinStart() {
		
		logMessage("Started with parameter: " + getAddinParameters());
		
		try {
			logMessage("Running on " + dbGetSession().getNotesVersion());
		} catch (Exception e) {
			logMessage("Unable to get Domino version: " + e.getMessage());
		}
		
		// Stay in main loop until termination signal set by addinStop()
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
		logMessage("You have entered the command: " + command);
	}
}
```

### **HCL Domino Console**

```
> Load RunJava JAddin HelloWorld
15.06.2025 15:21:17   JVM: Java Virtual Machine initialized.
15.06.2025 15:21:17   RunJava: Started JAddin Java task.
15.06.2025 15:21:18   HelloWorld: Started with parameter: null
15.06.2025 15:21:18   HelloWorld: Running on Release 14.0FP4 March 10, 2025
15.06.2025 15:21:18   HelloWorld: User code is executing...
15.06.2025 15:21:23   HelloWorld: User code is executing...
15.06.2025 15:21:28   HelloWorld: User code is executing...
> Tell HelloWorld Quit
15.06.2025 15:21:35   HelloWorld: Termination in progress
15.06.2025 15:21:36   HelloWorld: Terminated
15.06.2025 15:21:38   RunJava: Finalized JAddin Java task.
15.06.2025 15:21:39   RunJava shutdown.
```

### **Prerequisites**

* HCL Domino 9.0.1 FP8 or higher (Java Virtual Machine 1.8+ requirement)

### Credits

Photo by [Markus Spiske](https://unsplash.com/ja/@markusspiske?utm_source=unsplash\&utm_medium=referral\&utm_content=creditCopyText) on [Unsplash](https://unsplash.com/de/s/fotos/java-programming?utm_source=unsplash\&utm_medium=referral\&utm_content=creditCopyText)

### **Author**

This framework was created to help implementing projects which required the use of HCL Domino server add-ins. If you encounter any issue or if you have a suggestion, please let me know.

You may contact me thru my email address [andy.brunner@k43.ch](mailto:andy.brunner@k43.ch).

### **Unlicense (see** [**Wikipedia:Unlicense**](https://en.wikipedia.org/wiki/Unlicense)**)**

> Created with love and passion in the beautiful country of 🇨🇭 Switzerland. This software shall be used for Good not Evil. As far as I know, no animal was harmed in the making of this software 😊
