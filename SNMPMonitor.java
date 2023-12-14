package com.Snmp.conf;

//SnmpMonitor.java
//Main Program

import java.util.Scanner;

public class SNMPMonitor {

 public static void main(String[] args) {
     Scanner scanner = new Scanner(System.in);

     System.out.print("Enter SNMP Agent IP address: ");
     String ipAddress = scanner.nextLine();

     System.out.print("Enter SNMP Community String: ");
     String communityString = scanner.nextLine();

     System.out.print("Enter CSV file name: ");
     String csvFileName = scanner.nextLine();

     System.out.print("Enter time interval in minutes: ");
     long timeInterval = scanner.nextLong() *60 * 1000; // Convert minutes to milliseconds

     SnmpProcessor snmpProcessor = new SnmpProcessor(ipAddress, communityString, csvFileName, timeInterval);
     snmpProcessor.startMonitoring();
 }
}

