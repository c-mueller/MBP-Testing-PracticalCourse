#!/bin/bash
runningPID=$(ps -ef | grep sensoradapter_gpspl_hight.jar | grep -v grep | awk '{print $2}');
if [[ $runningPID != "" ]]; then
   echo "true";
else
   echo "false";
fi