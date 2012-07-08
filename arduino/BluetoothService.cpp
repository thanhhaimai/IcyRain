#include "BluetoothService.h"

BluetoothService::BluetoothService(SoftwareSerial* device) {
  message.data = (byte*) malloc(128);
  bluetooth = device;
}

BluetoothService::~BluetoothService() {
  free(message.data);
}

Message* BluetoothService::getMessage() {
  if (size == 0) {
    if (bluetooth->available() >= 3) {
      if (bluetooth->read() != 0x2F) {
        return NULL;
      }
      if (bluetooth->read() != 0x75) {
        return NULL;
      }
      message.opCode = (byte) bluetooth->read();
      size = (byte) bluetooth->read();
      index = 0;
    }
  } else {
    while (bluetooth->available() > 0 && index < size) {
      message.data[index] = bluetooth->read();
      index++;
    }

    if (index != size) {
      return NULL;
    }

    byte checkSum = (byte) bluetooth->read();
    for (int i = 0; i < size; i++) {
      checkSum ^= message.data[i];
    }

    if (checkSum == size) {
      Serial.println("pass checksum");
      message.size = size;
      size = 0;
      return &message;
    } else {
      Serial.println("do not pass checksum");
      size = 0;

    }
  }   
  return NULL;
}

void BluetoothService::sendMessage(Message* message) {
  bluetooth->write(0x2F);
  bluetooth->write(0x75);
  bluetooth->write(message->opCode);
  bluetooth->write(message->size);

  byte checkSum = message->size;
  for (int i = 0; i < message->size; i++) {
    checkSum ^= message->data[i];
    bluetooth->write(message->data[i]);
  }
  bluetooth->write(checkSum);
}
