#!/bin/bash
LIB_FILES=src
for f in `find . -name "*.jar"`
do
  LIB_FILES=$LIB_FILES:$f
done
java -cp $LIB_FILES com.acertainbank.BankServer 8081 0
