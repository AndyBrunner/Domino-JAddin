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
```

### **HCL Domino Console**

```
> Load RunJava JAddin HelloWorld
21.06.2025 14:01:26   JVM: Java Virtual Machine initialized.
21.06.2025 14:01:26   RunJava: Started JAddin Java task.
21.06.2025 14:01:26   HelloWorld: Started
21.06.2025 14:01:26   HelloWorld: User code is executing...
21.06.2025 14:01:31   HelloWorld: User code is executing...
21.06.2025 14:01:36   HelloWorld: User code is executing...
21.06.2025 14:01:41   HelloWorld: User code is executing...
> Tell HelloWorld Quit
21.06.2025 14:01:50   HelloWorld: Termination in progress
21.06.2025 14:01:51   HelloWorld: Terminated
21.06.2025 14:01:53   RunJava: Finalized JAddin Java task.
21.06.2025 14:01:54   RunJava shutdown.
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
