
// Included for serial communication

#include "messages.h"
#include "BluetoothService.h"

#include <SoftwareSerial.h>

typedef unsigned char u8;
typedef unsigned int u32;

// Define pins you're using for serial communication
// for the BlueSMiRF connection
#define TXPIN 3
#define RXPIN 2

// Create an instance of the software serial obj
SoftwareSerial bluesmirf(RXPIN, TXPIN);
BluetoothService bluetooth(&bluesmirf);

static struct Message idle_msg;

const short motor_map[] = {
	/* motor_id => PIN number */
	0,
	1,
	2,
	3,
	4,
	5,
	6,
	7,
	8,
	9,
	10,
	11,
	12,
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

  // Initialize the "idle" message
  idle_msg.opCode = IDLE;
  idle_msg.size = 0;

  // Might as well let the server know we're open for business
  bluetooth.sendMessage(&idle_msg);
}

// Main application loop
void loop()
{
  // Get sensor data
  // XXX

  // Wait for command-line input
  Message* message = bluetooth.getMessage();
  if (message != NULL)
  {
    handleMessage(message);
  }

  // Let the server know we're free
  bluetooth.sendMessage(&idle_msg);

  delay(100);
}

void send_vibrate(unsigned char motor_id, unsigned char magnitude) {
  short pin_nr = motor_map[motor_id];
  analogWrite(pin_nr, magnitude);
}

void vibrate(Message* msg) {
  /* Vibrate command format:
   *
   * [ le-u32:	duration ]
   * [ u8:	nr_motors ]
   * <[ u8:	motor_id]
   * [ u8:	magnitude ]> (nr_motors)
   */

  byte* field = msg->data;
  #define FIELD_GET(_lhs, _type) \
    _type _lhs = *((_type*) field); \
    field = field + sizeof(_type); \

  FIELD_GET(duration, u32);
  FIELD_GET(nr_motors, u8);
  byte* motor_tuples = field;

  for (byte i=0; i < nr_motors; ++i) {
    FIELD_GET(motor_id, u8);
    FIELD_GET(magnitude, u8);
    send_vibrate(motor_id, magnitude);
  }
  
  delay(duration);

  field = motor_tuples;

  for (byte i=0; i < nr_motors; ++i) {
    FIELD_GET(motor_id, u8);
    FIELD_GET(magnitude, u8);
    send_vibrate(motor_id, magnitude);
  }
  
  #undef FIELD_GET
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
      vibrate(message);
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
