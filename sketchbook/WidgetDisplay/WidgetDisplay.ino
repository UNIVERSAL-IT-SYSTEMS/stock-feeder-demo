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
const int top = 50;
Label stocks[stockCount] = { 
  Label(lcd, 5, top+5, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, ""),
  Label(lcd, 5, top+30, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, ""),
  Label(lcd, 5, top+55, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, ""),
  Label(lcd, 5, top+80, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, ""),
  Label(lcd, 5, top+105, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, ""),
  Label(lcd, 5, top+130, SGC_FONT_SIZE.LARGEST, SGC_COLORS.WHITE, SGC_COLORS.BLACK, ""),
};

Label app(lcd, 5, 5, SGC_FONT_SIZE.SMALL, SGC_COLORS.GREEN, SGC_COLORS.BLACK, "GorillaBuilderz Stock Streamer");
Label site(lcd, 5, 15, SGC_FONT_SIZE.SMALL, SGC_COLORS.GREEN, SGC_COLORS.BLACK, "http://gorillabuilderz.com.au");
Label status(lcd, 5, 310, "Waiting for update");

void setup() {
  delay(3000);
  lcd.initialise();

  // Draw each item
  for(int index = 0; index < stockCount; index++) {
    stocks[index].draw();
  }

  app.draw();
  site.draw();
  status.draw();

  delay(2000);
}

void parseLine(String line) {
  char direction = line[15];
  // Check if the line is the format we are after
  if(line.length() < 11 ||
      (direction != '+' && direction != '-' && direction != '/')) {
    return;
  }
  
  status.setText("Receiving data...");      

  int i = line[0] - 48;
  
  if(direction == '+') {
    stocks[i].setColor(SGC_COLORS.GREEN);
  }
  else if(direction == '-') {
    stocks[i].setColor(SGC_COLORS.RED);    
  }
  else {
    stocks[i].setColor(SGC_COLORS.WHITE);
  }
  
  stocks[i].setText(line.substring(2, 13));
  
  status.setText("Waiting for update");
}

void loop() {
  parseLine("1 REN   57.159 +");
  parseLine("0 FURA 325.000 -");  
  delay(1000);
  parseLine("1 REN   57.005 -");  
  delay(1000);
  parseLine("1 REN   57.159 /");    
  delay(1000);  
}

