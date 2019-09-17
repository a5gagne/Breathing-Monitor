//This example code is in the Public Domain (or CC0 licensed, at your option.)
//By Evandro Copercini - 2018
//
//This example creates a bridge between Serial and Classical Bluetooth (SPP)
//and also demonstrate that SerialBT have the same functionalities of a normal Serial

#include "BluetoothSerial.h"

BluetoothSerial SerialBT;

void setup() {
  SerialBT.begin("AGagne_ESP32"); //Bluetooth device name
}

void loop() {

//  if (SerialBT.available()) {
//    Serial.write(SerialBT.read());
//  }
  
  SerialBT.println(floatMap(analogRead(A2),0,4095,0,3.3),2);

  delay(10);
}

float floatMap(float x, float inMin, float inMax, float outMin, float outMax) {
  return (x-inMin)*(outMax-outMin)/(inMax-inMin)+outMin;
}
