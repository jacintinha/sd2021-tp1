#!/bin/bash
./test-sd-tp2.sh -image sd2021-tp2-55075-55697 -test 114a -sleep 3 -log ALL # timeout 1 # -pretty true
docker kill $(docker ps -a -q) 
