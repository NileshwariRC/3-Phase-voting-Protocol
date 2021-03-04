# 3_Phase_Commit_Protocol
3 Phase Voting Protocol using Socket Programming

Software Design Document

#Overview

This project is an implementation of 3 Phase Voting Protocol using Socket Programming. Below are the features of the project:
•	Application includes 7 Data servers and 5 Clients with communication channel between all servers and from each client to all servers
•	All communication channels are FIFO and reliable
•	Servers and Clients run at configurable host and port
•	Servers and clients are partitioned in groups and occasional disruption between channels is implemented using Proxy server
•	Each Client requests to Insert/Write an object(each text file is an object) on multiple servers determined using hash function

#Scenarios

•	When Client wants to WRITE:

o	For each randomly chosen object O(k), the write is performed on the 3 servers which are determined by using hash function – H(O(k)), H(O(k) + 1) % 7 and H(O(k) + 2) % 7.
o	If minimum 2 out of 3 servers are accessible from client, write request will be performed
Else, Operation will be aborted
o	Mutual exclusion is ensured when write request from multiple clients are received

•	When Client wants to READ:

o	Client can send Read request to any of the 3 replicas of an object
o	If object has not been inserted by any of the clients, error message will be displayed

•	When Proxy server selectively disables few communication channels:

o	All communications go through it even server to server communications. Write will be performed if all above mentioned requirements are satisfied

#Non-Goals

•	If updates are performed to two replicas of an object, and not to the third because the third replica was unreachable, later when the third replica becomes reachable, the missing update does not have to be performed on the third replica.

#Approach

•	The project has been designed using Java with multithreading at Client and Server
•	The Server is created as separate java class which is deployed on 7 different servers which are needed to be started first
•	Each Client is a separate java class which is deployed on 5 different servers.
•	Whenever a new client starts, it sends read/write requests to 3 servers for a random object O(k)
o	When client connects to the server, a separate thread is created from the server to listen to the incoming messages from the newly connected client 
o	The connection management between client and server is carried out using socket creation and its methods to accept the connection request and close the resources

#Flowchart


 ![image](https://user-images.githubusercontent.com/46851071/110028489-2c27e100-7ce8-11eb-8a4b-96985ea2a2c8.png)


#Correctness

1.	Random value selection:
o	Client selects any random value from 1 – 7
o	Based on this random value, client determines the object file_name.txt and the servers to which request will be sent using below hash functions

2.	Hash value and server list:
o	For the random value generated, list of 3 servers is created using below calculations
H(random_value), H(random_value + 1) % 7, H(random_value + 1) % 7
o	Client sends WRITE request to these 3 servers and determines if quorum of minimum 2 servers is received to commit requested changes using ‘3 phase voting protocol’ explained below
o	If less than 2 servers are accessible, the changes are aborted

3.	Voting Protocol:
o	Project uses 3 Phase Voting Protocol for permitting writes to servers
o	Each Client selects random values as mentioned in above points.
o	Based on random value, object name(file_name.txt) and quorum of size 3 is selected to perform ‘write’ request on this quorum.
o	Client sends Write request to 3 servers, the further flow is explained through flowchart.

#Safety

•	Mutual Exclusion: 
Each Server receives Write/Insert request from multiple clients. If previous request is already in process, and other client has already received ‘Grant’ from the server, the server state is ‘Locked’, requesting client receives Reject from the server else, Client sends write request to 3 servers.
o	Out of these 3 servers, 1 is chosen as Coordinator provided that quorum size is already known when Write request received from Client and other servers act as Cohorts.
o	Coordinator selection is random
•	File write is synchronized and only 1 client is able to update file at a time

#Liveliness

•	Timeouts:
1.	When Client sends a Write request and didn’t receive success response from servers for 15000 Milliseconds, it is considered that server is busy or failed. Client checks quorum size and if it is more than 1, write request is sent to available and accessible servers.
2.	Client waits for quorum for 10000 Milliseconds. If quorum size is less than 2, Error message “Request failed: Client unable to get Quorum” is displayed and request is aborted.
3.	When Coordinator sends ‘Prepare’ message to all cohorts, it changes its state to ‘wait’. If Ack is not received from a cohort, coordinator waits for 5000 Milliseconds for each else it sends message "Request Failed" to Client

REPORT

Code Execution Snapshots
Client-  DC11 – DC15
Server DC02 - DC08 
1.	Client1 sent Write request 

  ![image](https://user-images.githubusercontent.com/46851071/110028649-5d081600-7ce8-11eb-9637-3c906d8cfaa7.png)

Coordinator Server4 processing the request

 ![image](https://user-images.githubusercontent.com/46851071/110028668-62fdf700-7ce8-11eb-89f5-30cd29226903.png)

2.	Client 2 send READ request

 ![image](https://user-images.githubusercontent.com/46851071/110028683-685b4180-7ce8-11eb-83da-471453f8f55c.png)

Server 2 2.txt file

 ![image](https://user-images.githubusercontent.com/46851071/110028695-6c875f00-7ce8-11eb-8901-7b07428dbebd.png)

Server 2 processing READ request

 ![image](https://user-images.githubusercontent.com/46851071/110028709-727d4000-7ce8-11eb-85ac-56eb02d37d43.png)

#Settings:
Config.properties contains the settings.
Setting name are intuitive to understand.
You can block proxy channel by setting SERVER(ID)_BLOCKED to TRUE
