# Distance Based Application Trigger
## Repository Overview
* `/Android` Home of Android Application
* `/RaspberryPi` Home of RaspberryPi application to create Beacon & Rest API
## Project Description
### Main idea
The main idea of this project is to build an app, that allows to trigger IoT devices based on position / distance. One standard way to do this, e.g. employed by IFTTT, is the use of GPS or the connectivity of WiFi networks 1 . Both of these technologies allow for great radius, but are quite limited in their accuracy. Thus we propose to use Bluetooth Low Energy to estimate the distance. Depending on the progress of the project it might also be feasible to triangulate WiFi signals to further improve the accuracy.
### Features
* Estimate distance to a given Bluetooth Beacon using Bluetooth Low Energy
* At given distance authenticate and trigger REST Api
### Use cases
* Switch on desk lamp at workplace when sitting down at the desk
* Activate door buzzer when approaching home (high requirement for accuracy, since the door buzzer should only be activated while immediately in front of the door and not continuously while at home.)
### Used Sensors
* Bluetooth Low Energy
### Architecture
* The Bluetooth Beacon can be simulated by a RaspberryPi, which will not be explicitly part of the project.
* There are different options for the REST APIs. It would be possible to use WiFi enabled Plugs or a Raspberry Pi.
* The focus will be on developing the Android Application or Web Application, that measures the distance.
