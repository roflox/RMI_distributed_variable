#!/bin/bash

java -jar ../target/nodeImpl-jar-with-dependencies.jar -n Node3 -p 50000 -r 1099 -h 192.168.56.103 -A 192.168.56.102 -t Node2 -P 1099
