# Capstone
Capstone - Breathing Monitor


Originally, our plan was to display the data on an Android phone using bluetooth. 
I was able to send accelerometer data, in real time, to the app using 
AGagne_AndroidApp.ino on an Arduino board. 

The other members were worried about their contributions to the project, so we decided to send our data to 
MATLAB instead. AGagne_BreathingMonitor.m is the resulting code that displays the filtered data, calculates the breathing rate, and classifies the breathing pattern. Slight changes were made in AGagne_BT.ino to send the data to the computer, rather than a phone.

The enclosure was designed in Fusion 360 and then 3D printed
