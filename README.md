# distributed-ledger-raft-consensus

This project is a distributed ledger prototype built on top of the Raft consensus protocol.
For ease of use and efficiency, Google's etcd key-value store has been used as the Raft consensus implementation.

To test the project, simply provide the number of transactions that you want to add to the distributed ledger's record.
In order to make sure that all replicas maintain the same record, each transaction is passed with an ID (random number) as the value,
but with the integer 1 as the key. Since etcd keeps track of all changes made to each key in the store, using 1 as the key for all transactions
allows us to consistently order the transactions.

Once a predetermined number of transactions have occurred (100 in this implementation), the Block data structure is created and the last 100 transactions
are stored. In case less than 100 transactions are left to be added to the blockchain, the Block comprises of however many transactions are remaining. These Blocks are written to local storage in the form of a LinkedList to ensure persistence, thereby providing a PoC implementation of a Blockchain that is distributed, scalable and secure.

Note that even though a client isn't implemented since this project focuses mainly on the functioning of the server and the use of consensus, it is important to consider some real-world edge cases. For instance, let’s say that the server is unable to reach the cluster; i.e. the cluster is offline for some reason. As per this implementation, the server will keep pinging the endpoints in order to attempt a key-value add, and will give up and terminate if 3 consecutive queries to the cluster fail. Moreover, if the server encounters 3 failures during transaction add, all previously committed transactions are also removed from the list, so that the client has to resubmit all of the transactions to the server again, and no partial commits are made (atomicity of transaction commit sequences). Not even a single block will be written in case of failure. In a real-world setting this would make sense since the server would try submitting transactions to the cluster, and post some consecutive failures the server concludes that there is some problem, contacts the client and lets the client know that none of the transactions have been added to the ledger because the cluster was offline. Thus, these transactions would not go through, and the client would have to try again later. It is critical to ensure ACID properties are adhered to, and keeping transactions “all or nothing” in my view is a good approach, since otherwise such a distributed system would not be a good choice for financial transactions and may have inconsistency.

Another detail to point out is that each Block stores the hash value of the previous Block, in order to maintain the integrity of the Blockchain. In this implementation, I have made the first Block (head of the LinkedList) something known as a Genesis Block, which has previous hash value = “0000000000”, and it acts as a predecessor for the actual first transaction. This makes sure that all blocks added to the blockchain have a valid value for the previous hash field, and is actually used in real-world implementations of a blockchain.

Finally, one last detail to point out is the nature of storage of records. In my prototype, I have used a LinkedList, which I serialize to a local file to maintain the local ledger on the server. However, in reality there are various approaches to achieving the same thing. In the easiest way, one could simply write the transaction blocks in text format (or JSON) to a text file, which would mean that appending entries involves simply adding an entry to the JSON file. Bitcoin in reality uses Google’s LevelDB database to store the metadata. There are various approaches, and I simply felt that a LinkedList models the blockchain in the most intuitive manner. However, if the size of the blockchain gets too large (millions of records), then using a database is the better approach, since even for adding an entry to the LinkedList the entire list object must be loaded to memory, which would be slow and likely impossible given memory constraints. 

