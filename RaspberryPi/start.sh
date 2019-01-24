#!/bin/bash

# Setup GPIOs of RaspberryPi
# Exports pin to userspace
echo "18" > /sys/class/gpio/export
sleep 2
# Sets pin 18 as an output
echo "out" > /sys/class/gpio/gpio18/direction
sleep 2

## Flash the output once to show that the program started
# Sets pin 18 to high
echo "1" > /sys/class/gpio/gpio18/value
sleep 2
# Sets pin 18 to low
echo "0" > /sys/class/gpio/gpio18/value

# Start Node JS Application
npm start