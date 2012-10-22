/*
  GorillaBuilderz Web client
 
 Circuit:
 * WiFi shield attached to pins A1, 2, 5, 6
 
 */

#include <SPI.h>

// Need to include GorillaBuilderz WizFi Ethernet libraries
#include <Transport.h>
#include <WizFi210.h>
#include <GBEthernet.h>
// Needed to read the GBIMAC, so you don't have to provide a MAC address....
#include <GBIMAC.h>

// NOTE: With GorillaBuilderz shield we have a a GBIMAC identifier you can use!
GBIMAC macReader(3);
byte mac[MAC_LENGTH];

IPAddress server(54,243,140,117); // stocks.gorillabuilderz.com.au

// Initialize the Ethernet client library
// with the IP address and port of the server 
// that you want to connect to (port 80 is default for HTTP):
EthernetClient client;

void setup() {
  // start the serial library:
  Serial.begin(115200);

  Serial.println("GorillaBuilderz Arduino WebClient for WiFi Shield");
  Serial.println("Initialising modem and ataching to network...");
  
  // NOTE: If you need to redefine the IO to your wifi shield call this BEFORE you execute any Ethernet* methods
  // WizFi210::create(A1, 2, 5, 6);
  
  // Set the network name
  Ethernet.ssid("BatPhone");
  // Initialise secure network passphrase
  Ethernet.passphrase("password");

  // Read in the GBIMAC address
  macReader.read(mac);
  
  // start the Ethernet connection:
  if (Ethernet.begin(mac) == 0) {
    Serial.println("Failed to configure Ethernet using DHCP");
    // no point in carrying on, so do nothing forevermore:
    for(;;)
      ;
  }
  // give the Ethernet shield a second to initialize:
  delay(1000);
  Serial.println("connecting...");

  // if you get a connection, report back via serial:
  if (client.connect(server, 9000)) {
    Serial.println("connected");
    // Make a HTTP request:
    client.println("GET /pollStream HTTP/1.0");
    client.println();
  } 
  else {
    // kf you didn't get a connection to the server:
    Serial.println("connection failed");
  }
}

void loop()
{
  // if there are incoming bytes available 
  // from the server, read them and print them:
  if (client.available()) {
    char c = client.read();
    Serial.print(c);
  }

  // if the server's disconnected, stop the client:
  if (!client.connected()) {
    Serial.println();
    Serial.println("disconnecting.");
    client.stop();

    // do nothing forevermore:
    for(;;)
      ;
  }
}

