#include "BluetoothSerial.h"

#define zaxis A2

BluetoothSerial SerialBT;

void setup() {
  SerialBT.begin("AGagne_ESP32"); //Bluetooth device name
}

void loop() {

  if (SerialBT.available()>0) {
    char read = SerialBT.read();
    
    switch(read){
      case 'E':
      start();
      break;
    }
  }
}

void start(){
  while(1){
    // SerialBT.print('s'); // ANDROID APP
    // SerialBT.print(floatMap(analogRead(zaxis),0,4095,0,3.3),2);
    
    SerialBT.println(floatMap(analogRead(A2),0,4095,0,3.3),2); // Matlab
    
    delay(10);

    if(SerialBT.available()>0){
      if(SerialBT.read()=='Q'){
        return;
      }
    }
  } // while
} // start

float floatMap(float x, float inMin, float inMax, float outMin, float outMax) {
  return (x-inMin)*(outMax-outMin)/(inMax-inMin)+outMin;
}
