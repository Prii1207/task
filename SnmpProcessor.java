package com.Snmp.conf;

//Importing Packages
import org.snmp4j.*;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


//Class SNMP Processor

public class SnmpProcessor {

    private static final int SNMP_VERSION = SnmpConstants.version2c;
    private static final int SNMP_RETRIES = 2;
    private static final long SNMP_TIMEOUT = 1500; // in milliseconds

    private final String ipAddress;
    private final String communityString;
    private final String csvFileName;
    private final long timeInterval;

    private boolean headerWritten = false;

    // Map to store OID-to-name mappings for attributes
    private static final Map<String, String> attributeMappings = new HashMap<>();

    static {
        attributeMappings.put("1.3.6.1.2.1.2.2.1.1", "InterfaceIndex");
        attributeMappings.put("1.3.6.1.2.1.2.2.1.2", "InterfaceName");
        attributeMappings.put("1.3.6.1.2.1.2.2.1.10", "InOctets");
        attributeMappings.put("1.3.6.1.2.1.2.2.1.16", "OutOctets");
    }

    public SnmpProcessor(String ipAddress, String communityString, String csvFileName, long timeInterval) {
        this.ipAddress = ipAddress;
        this.communityString = communityString;
        this.csvFileName = csvFileName;
        this.timeInterval = timeInterval;
    }

    
    //Using Timer Task
    public void startMonitoring() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new SnmpMonitoringTask(), 0, timeInterval);
    }
    
    

    // Monitoring SNMP by sending a GETBULK request to the specified
    // IP address with the provided community string and retrieves information
    //about network interfaces (InterfaceIndex, InterfaceName, InOctets, OutOctets)
    
    private class SnmpMonitoringTask extends TimerTask {
        @Override
        public void run() {
            try (TransportMapping<?> transport = new DefaultUdpTransportMapping(); Snmp snmp = new Snmp(transport)) {

                transport.listen();

                CommunityTarget target = new CommunityTarget();
                target.setCommunity(new OctetString(communityString));
                target.setVersion(SNMP_VERSION);
                target.setAddress(new UdpAddress(ipAddress + "/161"));
                target.setRetries(SNMP_RETRIES);
                target.setTimeout(SNMP_TIMEOUT);

                try {
                	
                	// Create a PDU for GETBULK request and add relevant OIDs
                    PDU pdu = new PDU();
                    pdu.setType(PDU.GETBULK);
                    pdu.setMaxRepetitions(4); // Adjust as needed/
                    pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.2.2.1.1"))); // InterfaceIndex
                    pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.2.2.1.2"))); // InterfaceName
                    pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.2.2.1.10"))); // InOctets
                    pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.2.2.1.16"))); // OutOctets
                    
                    // Send the PDU to the target and receive the response
                    ResponseEvent responseEvent = snmp.send(pdu, target);
                    PDU response = responseEvent.getResponse();

                    if (response != null) {
                        processResponse(response);
                    } else {
                        System.out.println("No response received.");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processResponse(PDU response) {
        VariableBinding[] vbs = response.toArray();

        try (FileWriter csvWriter = new FileWriter(csvFileName, true)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = sdf.format(new Date());
            int count = 0;

            for (VariableBinding vb : vbs) {
                if (!headerWritten) {
                    // Add headers only for the first iteration
                    writeHeader(csvWriter, vbs);
                    headerWritten = true;
                }

                // Add IP address, port number, and timestamp
                if (count == 0) {
                    csvWriter.append(timestamp);
                    csvWriter.append(",");
                    csvWriter.append(ipAddress);
                    csvWriter.append(",");
                    csvWriter.append("161");
                    csvWriter.append(",");
                }
                csvWriter.append(vb.getVariable().toString() + ",");
                count++;
                if (count == 4) {
                    csvWriter.append("\n");
                    count = 0;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeHeader(FileWriter csvWriter, VariableBinding[] vbs) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = sdf.format(new Date());
        csvWriter.append("Timestamp,");
        csvWriter.append("IPAddress,");
        csvWriter.append("PortNumber,");
        csvWriter.append("InterfaceIndex,");
        csvWriter.append("InterfaceName,");
        csvWriter.append("InOctets,");
        csvWriter.append("OutOctets");
        csvWriter.append("\n");
    }

    private String getAttributeName(OID oid) {
        String oidString = oid.toString();
        return attributeMappings.getOrDefault(oidString, oidString);
    }
}

