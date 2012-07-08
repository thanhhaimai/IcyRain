#ifndef __BLUETOOTHSERVICE_H__
#define __BLUETOOTHSERVICE_H__ 

#include "Arduino.h"

struct Message {
  byte opCode;
  byte size;
  byte* data;
};

class BluetoothService {
private:
  byte index;
  byte size;
  Stream* bluetooth;
  Message message;

public:
  BluetoothService(Stream* stream);
  ~BluetoothService();
  Message* getMessage();
  void sendMessage(Message* message);
};

#endif
