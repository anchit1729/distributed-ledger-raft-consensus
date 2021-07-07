package blockchain;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KV;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;

public class Server {
	
	// We keep a variable for debugging, printing etc.
	public static final boolean DEBUG = true;

	// We maintain 100 transactions per block
	public static final int blockSize = 100;
	
	// We grow a list to store the last 100 successful transactions
	public static ArrayList<Long> currentTx = new ArrayList<Long>();
	
	// We keep a counter to keep track of how many transactions have been completed successfully
	public static int txCounter = 0;
	
	// We keep a pseudo-key value to count the serial number of a transaction
	public static int keyCount = 1;
	
	// We store the blockchain in a local file called blockchain.dat
	public static final String filename = "blockchain.dat";
	
	// We keep track of the endpoints of our raft cluster; this is used to query the cluster from the server
	// By default, etcd makes sure all queries eventually get directed to the leader of the cluster
	public static final String endpoints = "http://localhost:12379,http://localhost:22379,http://localhost:32379";
	
    public static void main(String[] args)	{
    	
    	// require 1 argument - the number of transactions to carry out SUCCESSFULLY
    	if (args.length != 1) {
    		System.out.println("Error. Expected arguments - {number of transactions to execute successfully}");
    		System.exit(0);
    	}
    	
    	// set the number of transactions to be carried out
    	int txCount = Integer.parseInt(args[0]);
    	System.out.println("Commencing transaction sequence...");
    	// we count the number of cluster-contact failures
    	int failCount = 0;
    	// we count the number of blocks written to the blockchain
    	int numberOfWrites = 0;
    	
    	for (int i = 0; i < txCount; i++)	{
    		if(!executeTransaction() && failCount < 3)	{
    			// failed transactions are not counted as part of our completed transaction list
    			// we allow a maximum of 3 consecutive failures before the server gives up and cancels the transactions
    			i--;
    			failCount++;
    		} else	{
    			// increment the txCounter every time a transaction succeeds
    			if (failCount == 3) {
    				// 3 consecutive failures, terminate the program (cancel transaction)
    				System.out.println("Encountered 3 consecutive failures. Transaction sequence cancelled.");
    				deleteFromBlockChain(numberOfWrites);
    				System.exit(1);
    			}
    			// in case the call was successful, failCount is reset to 0 (since we want 3 'consecutive' fails)
    			failCount = 0;
    			txCounter++;
    			if (txCounter == 100 || i + 1 == txCount)	{
    				// either 100 transactions succeeded or the amount of transactions specified 
    				// have been carried out
    				txCounter = 0;
    				addBlockToChain();
    				numberOfWrites++;
    			}
    		}
    	}
    	
    	System.out.println("\n\n\n\n\n\n\n\n");
    	printBlockChain();
    }
    
    public static boolean executeTransaction()	{
    	Long randomTransactionKey = (long) keyCount;
    	Long randomTransactionContent = (long) ((Math.random() * 1000000000) + 1000000000);
    	try {
    		io.etcd.jetcd.Client client = io.etcd.jetcd.Client.builder().endpoints(endpoints.split(",")).build();
            KV kvClient = client.getKVClient();
            ByteSequence key = ByteSequence.from(randomTransactionKey.toString().getBytes(StandardCharsets.UTF_8));
            ByteSequence value = ByteSequence.from(randomTransactionContent.toString().getBytes(StandardCharsets.UTF_8));
			kvClient.put(key, value).get();
			client.close();
        } catch (Exception e)   {
            //System.out.println("ADD: Exception");
            return false;
        }
    	// transaction succeeded, add it to the list
    	currentTx.add(randomTransactionContent);
    	return true;
    }
    
    public static void deleteFromBlockChain(int numberOfWrites)	{
    	// this method implements a simple rollback of transactions if 3 consecutive failures are encountered
    	LinkedList<Block> list = readFromLedger();
    	for (int i = 0; i < numberOfWrites; i++) {
    		// remove all the blocks that were written during this execution
    		list.remove(list.size() - 1);
    	}
    	writeToLedger(list);
    }
    
    public static void addBlockToChain()	{
    	// method to create a block out of transactions in the currentTx list
    	// and save them to the chain
    	
    	// first, create a block
    	Block block = new Block(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()), currentTx, getPreviousHashValue());
    	currentTx.clear();
    	
    	if (DEBUG) {
    		// print out the contents of the block
    		System.out.println(block.toString());
    	}
    	
    	// now, add the created block to the LinkedList
    	LinkedList<Block> list = readFromLedger();
    	list.add(block);
    	writeToLedger(list);
    	
    }
    
    public static String getPreviousHashValue()	{
    	// to get the last hash value from the local ledger/blockchain
    	// if the ledger is empty, it returns an empty string
    	LinkedList<Block> list = readFromLedger();
    	if (!list.isEmpty())
    		return list.getLast().getHashValue();
    	else	
    		return "";
    	
    }
    
    @SuppressWarnings("unchecked")
	public static LinkedList<Block> readFromLedger()	{
    	
    	LinkedList<Block> list = new LinkedList<Block>();
    	
    	// check if file is empty; in such case return empty list
    	File file = new File("blockchain.dat");
    	if (file.length() == 0) {
    		// empty file 
    		// create the genesis block (transaction ID -1)
    		ArrayList<Long> genesisList = new ArrayList<Long>();
    		Long genesisVal = (long) -1;
    		genesisList.add(genesisVal);
    		Block genesis = new Block(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()), genesisList, "0000000000");
    		LinkedList<Block> newList = new LinkedList<Block>();
    		newList.add(genesis);
    		writeToLedger(newList);
    		return newList;
    	}
    	
    	try	{
    		FileInputStream fis = new FileInputStream("blockchain.dat");
    		ObjectInputStream ois = new ObjectInputStream(fis);
    		Object obj = ois.readObject();
    		list = (LinkedList<Block>) obj;
    	} catch(Exception e) {
    		System.out.println("Exception in ledger read: " + e);
    	}
    	return list;
    	
    }
    
    public static void writeToLedger(LinkedList<Block> list)	{
    	
    	try	{
    		FileOutputStream fos = new FileOutputStream("blockchain.dat");
    		ObjectOutputStream oos = new ObjectOutputStream(fos);
    		oos.writeObject(list);
    		fos.close();
    	} catch (Exception e) {
    		System.out.println("Exception in ledger write: " + e);
    	}
    	
    	
    }
    
    public static void printBlockChain()	{
    	
    	LinkedList<Block> list = readFromLedger();
    	for (Block block : list) {
    		System.out.println(block.toString());
    	}
    	
    }

}
