# RaspberryPi Beacon + REST API
## Requirements
* NodeJS V8 (BluetoothHCI does not yet support Node 10)
* Depending on the system setup the following packages are required and can be installed as follows: 'sudo apt-get install bluetooth bluez libbluetooth-dev libudev-dev build-essential'
## Testing
The Node application is build to run on any linux machine. Thus can be used to create a local bluetooth beacon using a laptop.
## Deployment
The application can be deployed using Balena. The corresponding Dockerfile Template is in the root. The steps for the deployment itself follow the instructions that can be found here: https://www.balena.io/docs/learn/getting-started/raspberrypi3/nodejs/<br />
Be aware that remote deployment can be quite time consuming, since the the complete images needs to be downloaded from the Balena Servers. In order to avoid this the local mode can be used: https://www.balena.io/docs/learn/develop/local-mode/
 