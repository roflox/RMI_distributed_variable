#!/bin/bash

java -jar ../target/nodeImpl-jar-with-dependencies.jar -n Node2 -p 50000 -r 1099 -h 192.168.56.102 -A 192.168.56.1 -t Node1 -P 1099
