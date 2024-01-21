---
description: How to download the JAddin framework
layout:
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

# ⬇ Download

## Downloads

### Version 2.1.3 2023-09-11

{% file src="../.gitbook/assets/JAddin-2.1.3.zip" %}

* Remove JVM version check
* Project web page moved to https://jaddin.k43.ch
* Added architecture diagramm in documentation

### **Version 2.1.2 2021-01-25**

{% file src="../.gitbook/assets/JAddin-2.1.2.zip" %}
Download
{% endfile %}

* Change: Show OS, JVM and Domino versions on console in debug mode
* Change: Add Domino statistic “AddinName.JVM.HeapUsedKB”
* Documentation: The documentation has been updated and moved to GitBook.

### **Version 2.1.1 2019-03-07**

{% file src="../.gitbook/assets/JAddin-2.1.1.zip" %}
Download
{% endfile %}

* Change: dbSendMessage now uses mail1.box if mail.box is not present

### **Version 2.1.0 2019-02-03**

{% file src="../.gitbook/assets/JAddin-2.1.0.zip" %}
Download
{% endfile %}

* Added static methods JAddin.fromISODateUTC() and JAddin.toISODateUTC()
* Added method generateHash(), encryptAES(), decryptAES(), fromBase64 and toBase64()
* Changed method dbSendMessage() to always create a MIME message
* Changed method dbRecycleObjects() to better support arrays and vectors
* Changed Domino statistic to show date in UTC ISO 8601 format
* JavaDoc changes

### **Version 2.0.0 2019-01-16**

{% file src="../.gitbook/assets/JAddin-2.0.0.zip" %}
Download
{% endfile %}

* Major rewrite of JAddin.java and JAddinThread.java
* Now requires JVM 1.8+ (Domino 9.0.1 FP8+)
* Many new and changed methods to support applications
* Complete rewrite of the documentation and publish it on GitHub
* Create project homepage at https://jaddin.k43.ch

### **Version 1.3.0 2016-03-24**

* Beta Version for selected customers
* Change: Replace the Thread termination sequence by Thread.interrupt()
* Change: Mark addInTerminate() as deprecated
* Change: Small change in sendMessage() for processing sender name
* Add: New method waitSeconds() to delay execution
* Add: New method recycleObjects() to free Domino object resources
* Add: New documentation chapter to explain program flow

### **Version 1.2.0 2013-12-18**

* Add: New command “Version!” to display JAddin, Java and OS version numbers.
* Change: Send the low heap memory warning message only once for each threshold reached.

### **Version 1.1.0 2012-07-18**

* Add: New methods addinNextHour() and addinNextDay() to allow for notification of next hour and next day.
* Change: Several runtime optimizations.

### **Version 1.0.0 2012-04-28**

* Add: First formal version
* Add: New method sendMessage() to create and send a message.

### **Version 0.5.0 2010-08-22**

* New: First public beta version.
