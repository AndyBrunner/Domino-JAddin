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

{% hint style="success" %}
**Support for HCL Domino 14.5 and higher (JVM with Java 21) is implemented in** [**JAddin 2.1.4**](download/download.md)
{% endhint %}

{% hint style="info" %}
Follow development at Mastodon [#DominoJAddin](https://swiss.social/tags/dominojaddin)
{% endhint %}

## Introduction

The free and open-source JAddin framework acts as a thin and easy to use layer between the HCL Domino RunJava task and your Java application code. It helps you to create Java server tasks by freeing you to learn all the HCL Domino add-in specifics, such as message queue handling, thread creation, communication with the console, resource cleanup, etc. It is written entirely in Java to support all HCL Domino versions and platforms (HCL Domino 9.0.1 FP8 and above).

{% hint style="info" %}
This tool is also shown on the [_HCL Domino Marketplace_](https://hclsofy.com/domino)
{% endhint %}

{% hint style="success" %}
**JAddin is fully compatible with HCL Domino 14**

When JAddin is loaded, you will see the message _WARNING: A terminally deprecated method in java.lang.System has been called._ This is a normal message and can be ignored as written in the [HCL Domino Release Notes](https://help.hcltechsw.com/domino/14.0.0/admin/wn\_components\_no\_longer\_included\_in\_release.html).
{% endhint %}

### **Code Example**

```java
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
```

### **HCL Domino Console**

```
> Load RunJava JAddin HelloWorld
03.09.2024 15:31:28   JVM: Java Virtual Machine initialized.
03.09.2024 15:31:28   RunJava: Started JAddin Java task.
03.09.2024 15:31:29   HelloWorld: Started with parameters null
03.09.2024 15:31:29   HelloWorld: Running on Release 10.0.1 November 29, 2018
03.09.2024 15:31:29   HelloWorld: User code is executing...
03.09.2024 15:31:34   HelloWorld: User code is executing...
03.09.2024 15:31:39   HelloWorld: User code is executing...
03.09.2024 15:31:44   HelloWorld: User code is executing...
> Tell HelloWorld Quit
03.09.2024 15:31:53   HelloWorld: Termination in progress
03.09.2024 15:31:53   HelloWorld: Terminated
03.09.2024 15:31:55   RunJava: Finalized JAddin Java task.
03.09.2024 15:31:56   RunJava shutdown.
```

### **Prerequisites**

* HCL Domino 9.0.1 FP8 or higher (Java Virtual Machine 1.8+ requirement)

### Credits

Photo by [Markus Spiske](https://unsplash.com/ja/@markusspiske?utm\_source=unsplash\&utm\_medium=referral\&utm\_content=creditCopyText) on [Unsplash](https://unsplash.com/de/s/fotos/java-programming?utm\_source=unsplash\&utm\_medium=referral\&utm\_content=creditCopyText)

### **Author**

This framework was created to help implementing projects which required the use of HCL Domino server add-ins. If you encounter any issue or if you have a suggestion, please let me know.

You may contact me thru my email address [andy.brunner@k43.ch](mailto:andy.brunner@k43.ch).

### **Unlicense (see** [**Wikipedia:Unlicense**](https://en.wikipedia.org/wiki/Unlicense)**)**

> Created with love and passion in the beautiful country of 🇨🇭 Switzerland. This software shall be used for Good not Evil. As far as I know, no animal was harmed in the making of this software 😊
