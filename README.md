# distributed-ledger-raft-consensus

This project is a distributed ledger prototype built on top of the Raft consensus protocol.
For ease of use and efficiency, Google's etcd key-value store has been used as the Raft consensus implementation.

To test the project, simply provide the number of transactions that you want to add to the distributed ledger's record.
In order to make sure that all replicas maintain the same record, each transaction is passed with an ID (random number) as the value,
but with the integer 1 as the key. Since etcd keeps track of all changes made to each key in the store, using 1 as the key for all transactions
allows us to consistently order the transactions.

Once a predetermined number of transactions have occurred (100 in this implementation), the Block data structure is created and the last 100 transactions
are stored. These Blocks are written to local storage in the form of a List, thereby providing a PoC implementation of a Blockchain that is distributed, scalable
and secure.
