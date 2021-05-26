#!/bin/bash
./start-kafka.sh localhost&
sleep 5
./test-sd-tp2.sh -image sd2021-tp2-55075-55697 -test 101a -sleep 4 -timeout 1 -log ALL
docker kill $(docker ps -a -q) 
