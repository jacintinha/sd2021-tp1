#!/bin/bash
#./start-kafka.sh localhost&
#sleep 5
./test-sd-tp2.sh -image sd2021-tp2-55075-55697 -pretty true -test 108d -sleep 3 -timeout 1 -log ALL
#docker kill $(docker ps -a -q) 
