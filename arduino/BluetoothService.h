#ifndef __BLUETOOTHSERVICE_H__
#define __BLUETOOTHSERVICE_H__ 

#include "Arduino.h"
#include <SoftwareSerial.h>

struct Message {
  byte opCode;
  byte size;
  byte* data;
};

class BluetoothService {
private:
  byte index;
  byte size;
  SoftwareSerial* bluetooth;
  Message message;

public:
  BluetoothService(SoftwareSerial* device);
  ~BluetoothService();
  Message* getMessage();
  void sendMessage(Message* message);
};

#endif
