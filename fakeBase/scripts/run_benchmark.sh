#!/bin/bash

# Compile o código
javac -d bin -sourcepath src $(find src -name "*.java")

# Executa o benchmark
java -cp bin tests.Benchmark
