/*
  GorillaBuilderz Web client
 
 Circuit:
 * WiFi shield attached to pins A1, 2, 5, 6
 
 */

#include <SPI.h>

// Need to include GorillaBuilderz WizFi Ethernet libraries
#include <GBStatus.h>
#include <Transport.h>
#include <WizFi210.h>
#include <GBEthernet.h>
#include <GB4DLcdDriver.h>
#include <GB4DGraphics.h>
#include <GString.h>
// Needed to read the GBIMAC, so you don't have to provide a MAC address....
#include <GBIMAC.h>

const bool DEBUG = false;

// NOTE: With GorillaBuilderz shield we have a a GBIMAC identifier you can use!
GBIMAC macReader(3);
byte mac[MAC_LENGTH];

IPAddress server(54,243,140,117); // stocks.gorillabuilderz.com.au

// Initialize the Ethernet client library
// with the IP address and port of the server 
// that you want to connect to (port 80 is default for HTTP):
EthernetClient client;

GB4DSPILcdDriver lcd(A3);

const int stockCount = 6;
// AEX.AS
// ANZ.AX,BHP.AX,CBA.AX,NAB.AX,QAN.AX,RIO.AX
String stocksLabel[stockCount] = { "ANZ", "BHP", "CBA", "NAB", "QAN", "RIO", };
Label stocks[stockCount] = { 
  Label(lcd, 5, 5, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, "ANZ"),
  Label(lcd, 5, 30, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, "BHP"),
  Label(lcd, 5, 55, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, "CBA"),
  Label(lcd, 5, 80, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, "NAP"),
  Label(lcd, 5, 105, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, "QAN"),
  Label(lcd, 5, 130, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, "RIO"),
};

Label status(lcd, 5, 300, "Waiting for update");


void setup() {
  // start the serial library:
  Serial.begin(115200);
  
  lcd.initialise();

  // Draw each item
  for(int index = 0; index < stockCount; index++) {
    stocks[index].draw();
  }

  status.setText("Initialising WiFi...");
  status.draw();

  if(DEBUG) Serial.println("GorillaBuilderz Arduino WebClient for WiFi Shield");
  if(DEBUG) Serial.println("Initialising modem and ataching to network...");
  
  // NOTE: If you need to redefine the IO to your wifi shield call this BEFORE you execute any Ethernet* methods
  WizFi210::create(10, 2, 5, 6);
  
  // Set the network name
  Ethernet.ssid("BatPhone");
  // Initialise secure network passphrase
  Ethernet.passphrase("password");

  // Read in the GBIMAC address
  macReader.read(mac);
  
  // start the Ethernet connection:
  if (Ethernet.begin(mac) == 0) {
    if(DEBUG) Serial.println("Failed to configure Ethernet using DHCP");
    status.setText("Failed to associate to network");
    // no point in carrying on, so do nothing forevermore:
    for(;;)
      ;
  }
  
  status.setText("Network associated");
}

void parseLine(String line) {
  // Check if the line is the format we are after
  if(line.length() < 11 ||
      (line[10] != '+' && line[10] != '-' && line[10] != '/')) {
    if(DEBUG) Serial.print("No data: ");
    if(DEBUG) Serial.println(line.length());    
    return;
  }
  
  status.setText("Receiving data...");      

  int i = line[0] - 48;
  
  if(line[10] == '+') {
    stocks[i].setColor(SGC_COLORS.GREEN);
  }
  else if(line[10] == '-') {
    stocks[i].setColor(SGC_COLORS.RED);    
  }
  else {
    stocks[i].setColor(SGC_COLORS.WHITE);
  }
  
  stocks[i].setText(stocksLabel[i] + line.substring(1, 9));
  
  status.setText("Waiting for update");      
}

void loop()
{
  // if the server's disconnected, stop the client:
  if (!client.connected()) {
    if(DEBUG) Serial.println("connecting...");
    status.setText("Connecting...");
    
    // if you get a connection, report back via serial:
    if (client.connect(server, 80)) {
      if(DEBUG) Serial.println("connected");
      status.setText("Connected");
      // Make a HTTP request:
      client.println("GET /pollStream HTTP/1.0");
      client.println();
    } 
    else {
      // kf you didn't get a connection to the server:
      if(DEBUG) Serial.println("connection failed");
      status.setText("Connection failed");      
    }
  }
  else {
    // if there are incoming bytes available 
    // from the server, read them and print them:
    if (client.available()) {
      String string = client.readStringUntil('\n');
      if(DEBUG) Serial.println(string);
      parseLine(string);
    }
  }
}

