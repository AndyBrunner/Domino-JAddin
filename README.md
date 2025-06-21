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

# JAddin Java Framework for HCL Domino (Open Source)

<a href="download/download.md" class="button primary">Free Download</a>

{% hint style="info" %}
Follow development at Mastodon [#DominoJAddin](https://swiss.social/tags/dominojaddin)

This tool is also shown on the [_HCL Domino Marketplace_](https://hclsofy.com/domino)
{% endhint %}

## Introduction

The open-source JAddin framework serves as a lightweight and user-friendly layer between the HCL Domino RunJava task and your Java application code. It simplifies the creation of Java server tasks by abstracting the complexities of HCL Domino add-in development, such as message queue handling, thread management, console communication, and resource cleanup. Written entirely in Java, JAddin supports all HCL Domino versions and platforms starting from version 9.0.1 FP8 and above.

### **Code Example**

```java
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
```

### **HCL Domino Console**

```
> Load RunJava JAddin HelloWorld
16.06.2025 16:19:13   JVM: Java Virtual Machine initialized.
16.06.2025 16:19:13   RunJava: Started JAddin Java task.
16.06.2025 16:19:13   HelloWorld: Started with parameter: null
16.06.2025 16:19:13   HelloWorld: Running on Release 14.0FP4 March 10, 2025
16.06.2025 16:19:13   HelloWorld: User code is executing...
16.06.2025 16:19:18   HelloWorld: User code is executing...
16.06.2025 16:19:23   HelloWorld: User code is executing...
16.06.2025 16:19:28   HelloWorld: User code is executing...
> Tell HelloWorld Quit
16.06.2025 16:19:34   HelloWorld: Termination in progress
16.06.2025 16:19:35   HelloWorld: Terminated
```

### **Prerequisites**

* HCL Domino 9.0.1 FP8 or higher (Java Virtual Machine 1.8+ requirement)

### Credits

Photo by [Markus Spiske](https://unsplash.com/ja/@markusspiske?utm_source=unsplash\&utm_medium=referral\&utm_content=creditCopyText) on [Unsplash](https://unsplash.com/de/s/fotos/java-programming?utm_source=unsplash\&utm_medium=referral\&utm_content=creditCopyText)

### **Author**

This framework was created to support projects that require the use of HCL Domino server add-ins. If you encounter any issues or have suggestions for improvement, please feel free to reach out.

You may contact me thru my email address [andy.brunner@k43.ch](mailto:andy.brunner@k43.ch).

### **Unlicense (see** [**Wikipedia:Unlicense**](https://en.wikipedia.org/wiki/Unlicense)**)**

> Created with love and passion in the beautiful country of 🇨🇭 Switzerland. This software is intended to be used for good—not evil. And to the best of my knowledge, no animals were harmed in its creation. 😊
