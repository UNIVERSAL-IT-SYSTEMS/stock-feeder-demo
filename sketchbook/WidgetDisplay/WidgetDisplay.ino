#include <GBStatus.h>
#include <Transport.h>
#include <GB4DLcdDriver.h>
#include <SPI.h>
#include <GB4DGraphics.h>
#include <GString.h>

// Initialise the SPI driver using the default chip select. 
// NOTE: Remember to have the UART/SPI switch on SPI when uploading this sketch and running it
//       using the SPI driver!
GB4DSPILcdDriver lcd(A3);

// Uncomment the following line and comment above to use the serial driver
// NOTE: If using serial driver, remember to have the UART/SPI switch on SPI when uploading 
//       this sketch but switched to UART when running this sketch.
// GB4DSerialLcdDriver lcd;

// Justification
const int stockCount = 6;
String stocksLabel[stockCount] = { "ANZ", "BHP", "CBA", "QAN", "RIO", "CBA" };
Label stocks[stockCount] = { 
  Label(lcd, 5, 5, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, "ANZ"),
  Label(lcd, 5, 30, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, "BHP"),
  Label(lcd, 5, 55, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, "CBA"),
  Label(lcd, 5, 80, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, "QAN"),
  Label(lcd, 5, 105, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, "RIO"),
  Label(lcd, 5, 130, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, "CBA"),
};

Label status(lcd, 5, 300, "Waiting for update");

void setup() {
  delay(3000);
  lcd.initialise();

  // Draw each item
  for(int index = 0; index < stockCount; index++) {
    stocks[index].draw();
  }

  status.draw();

  delay(2000);
}

void parseLine(String line) {
  status.setText("Reading...");
  status.setText("Updating...");  
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

void loop() {
  parseLine("1  57.159 +");
  parseLine("0  25.000 -");  
  delay(1000);
  parseLine("1  57.005 -");  
  delay(1000);
  parseLine("1  57.159 /");    
  delay(1000);  
}

