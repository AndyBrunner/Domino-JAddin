---
description: Java Toolkit for developers writing HCL Domino Server Add-ins
cover: .gitbook/assets/Picture.jpg
coverY: -12
---

# JAddin Framework

## Introduction

The free and open-source JAddin framework acts as a thin and easy to use layer between the HCL Domino RunJava task and your Java application code. It helps you to create Java server tasks by freeing you to learn all the HCL Domino add-in specifics, such as message queue handling, thread creation, communication with the console, resource cleanup, etc. It is written entirely in Java to support all HCL Domino versions and platforms (HCL Domino 9.0.1 FP8 and above).

{% hint style="success" %}
**JAddin is fully compatible with HCL Domino 14**

When JAddin is loaded, you will see the message _WARNING: A terminally deprecated method in java.lang.System has been called._ This is a normal message and can be ignored as written in the [HCL Domino Release Notes](https://help.hcltechsw.com/domino/14.0.0/admin/wn\_components\_no\_longer\_included\_in\_release.html).
{% endhint %}

### **Code Example**

```java
public class HelloWorld extends JAddinThread {

	// This is the main entry point. When this method returns, the add-in terminates.
	public void addinStart() {
	
		logMessage("Started with parameters " + getAddinParameters());
		
		try {
			logMessage("Running on " + dbGetSession().getNotesVersion());
		} catch (Exception e) {
			logMessage("Unable to get Domino version: " + e.getMessage());
		}

		// Main add-in loop ...
		while (true) {
			logMessage("User code is executing...");
			waitMilliSeconds(15000L);
		}
	}

	// This method is called synchronously by the JAddin framework when the
	// command 'Quit' or 'Exit' is entered or at Domino server shutdown. Here
	// you may signal the addinStart() method to terminate and to perform any cleanup.
	public void addinStop() {
		logMessage("Termination in progress");
	}
	
	// This method is called asynchronously by the JAddin framework for any
	// console command entered.
	public void addinCommand(String command) {
		logMessage("You have entered the command " + command);
	}
}
```

### **HCL Domino Console**

```
> Load RunJava JAddin HelloWorld
03.02.2019 09:31:47   JVM: Java Virtual Machine initialized.
03.02.2019 09:31:47   RunJava: Started JAddin Java task.
03.02.2019 09:31:47   HelloWorld: Started with parameters null
03.02.2019 09:31:47   HelloWorld: Running on Release 10.0.1 November 29, 2018
03.02.2019 09:31:47   HelloWorld: User code is executing...
03.02.2019 09:32:02   HelloWorld: User code is executing...
03.02.2019 09:32:17   HelloWorld: User code is executing...
03.02.2019 09:32:32   HelloWorld: User code is executing...
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
