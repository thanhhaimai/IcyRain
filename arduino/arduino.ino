// Included for serial communication
#include "BluetoothService.h"

#include <SoftwareSerial.h>

// Define pins you're using for serial communication
// for the BlueSMiRF connection
#define TXPIN 3
#define RXPIN 2

// Create an instance of the software serial obj
SoftwareSerial bluesmirf(RXPIN, TXPIN);
BluetoothService bluetooth(&bluesmirf);

// Main application entry point 
void setup()

{
  // Define the appropriate input/output pins
  pinMode(RXPIN, INPUT);
  pinMode(TXPIN, OUTPUT);

  // Say something on the main serial.
  // This is mainly for debug purpose.
  Serial.begin(9600);
  Serial.println("Serial start!");

  // Begin communicating with the bluetooth interface.
  bluesmirf.begin(9600);
}

// Main application loop
void loop()
{
  // Wait for command-line input
  Message* message = bluetooth.getMessage();
  if (message != NULL)
  {
    handleMessage(message);
  }
  delay(100);
}

void handleMessage(Message* message) {
  switch(message->opCode) {
    case 10:
      // lets say opCode 10 is the echo service, just echo back
      bluetooth.sendMessage(message);
      break;
    case 9:
      // lets say opCode 9 is the print out service, just print back into serial monitor
      Serial.print("Receiving: ");
      for (int i = 0; i < message->size; i++) {
        Serial.print((char) message->data[i]);
      }
      Serial.println();
      break;
    default:
      break;
  }
}
