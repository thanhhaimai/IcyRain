
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

enum MessageOpCode {
  ECHO = 0,
  SERIAL_PRINT = 1,
  VIBRATE = 3,
  SET_SENSOR_STATE = 4,
  QUERY = 5,
};

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
    case ECHO:
      bluetooth.sendMessage(message);
      break;
    case SERIAL_PRINT:
      Serial.print("Receiving: ");
      for (int i = 0; i < message->size; i++) {
        Serial.print((char) message->data[i]);
      }
      Serial.println();
      break;
    case VIBRATE:
      // do vibrate
      break;
    case SET_SENSOR_STATE:
      // do set sensor state
      break;
    case QUERY:
      // return data for the query
      break;
    default:
      break;
  }
}
