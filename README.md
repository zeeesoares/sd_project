# Remote Memory Data Storage

## 📌 About the Project

The **Remote Memory Data Storage** project aims to implement a **shared data storage service**, where information is maintained on a server and accessed remotely by clients through **TCP sockets**. The server handles concurrent client requests and stores data in memory using a **key-value** structure. The project focuses on optimizing **concurrency**, **reducing contention**, and **minimizing thread usage**.

## 🚀 Technologies Used

- **Programming Language:** Java  
- **Networking:** TCP Sockets  
- **Concurrency:** Locks & Conditions  
- **Data Storage:** In-Memory Key-Value Store  
- **Testing & Benchmarking:** Custom Load Testing Framework  

## 📖 Features

✔️ Remote storage and retrieval of key-value pairs  
✔️ Support for **PUT**, **GET**, **MULTIPUT**, **MULTIGET**, and **GETWHEN** operations  
✔️ Optimized concurrency using fine-grained locks  
✔️ Custom protocol for efficient client-server communication  
✔️ Multi-client support with request queuing and thread pooling  
✔️ Performance testing with variable workloads  

## 👥 Team Members

- [André Miranda](https://github.com/RollingJack)  
- [Diogo Outeiro](https://github.com/DiogoOuteiro4)  
- [José Soares](https://github.com/zeeesoares)  
- [Nuno Melo](https://github.com/nunoMelo6)  

## 📊 Benchmarking & Testing
The system was tested under different workloads to evaluate performance and scalability:

- Read Heavy Workload: 90% read operations
- Write Heavy Workload: 90% write operations
- Balanced Workload: 50% read / 50% write operations
- Big Set vs Small Set: Tests with varying numbers of keys (1,000 vs 5,000)
- High vs Low Connection Load: Testing with different client concurrency levels


Results Summary

✔️ The system scales effectively with increasing threads  
✔️ Fine-grained locks reduce contention and improve concurrency  
✔️ Performance varies based on the number of stored keys  
✔️ Optimizations such as sharding could improve large-scale performance  

