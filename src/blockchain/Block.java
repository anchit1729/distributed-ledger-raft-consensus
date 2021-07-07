package blockchain;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * This class maintains the structure of a Block object, which is an object that is stored once
 * a pre-determined number of transactions are completed in a DLT implementation. 
 * 
 * @author anchitmishra
 *
 */
public class Block implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8263958694675271035L;

	// A Block contains the time when it was created
	private String timeStamp;
	
	// A Block contains the hash value of the previous block (this is to maintain consistency
	private String prevHashValue;
	
	// A Block contains the hash value of itself
	private String hashValue;
	
	// A Block contains the list of transactions it stores
	private ArrayList<Long> txList;
	
	/**
	 * A constructor to construct a Block with given timestamp and list of transactions + other parameters
	 */
	public Block(String timeStamp, ArrayList<Long> txList, String prevHashValue) {
		this.timeStamp = timeStamp;
		this.txList = new ArrayList<Long>();
		for (Long item: txList) {
			this.txList.add(item);
		}
		this.prevHashValue = prevHashValue;
		hashValue = this.computeHashValue();
	}
	
	
	public String computeHashValue() {
		
		// prepare the data string which is to be hashed
		String data = timeStamp + prevHashValue + hashValue;
		for (Long item : txList) {
			data += item.toString();
		}
		
		// now that the data string is ready, compute the hash
		MessageDigest messageDigest = null;
		byte[] bytes = null;
		try	{
			messageDigest = MessageDigest.getInstance("SHA-256");
			bytes = messageDigest.digest(data.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
	        System.out.println("Error in hash generation: " + e);
	    }
		
		StringBuffer stringBuffer = new StringBuffer();
		for (byte b : bytes) {
			stringBuffer.append(String.format("%02x", b));
		}
		
		return stringBuffer.toString();
	}
	
	public String getHashValue()	{
		return hashValue;
	}
	
	public String toString()	{
		String result = "";
		result += "Timestamp: " + timeStamp + "\n";
		result += "Prev. Hash: " + prevHashValue + "\n";
		result += "Hash Value: " + hashValue + "\n";
		result += "--------Transaction Contents--------\n";
		for (int i = 0; i < txList.size(); i++)	{
			result += i + ".: " + txList.get(i).toString() + "\n";
		}
		return result;
	}
	
	
}
