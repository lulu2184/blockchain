package lib;

import java.io.Serializable;

/**
 * Block Class, the element to compose a Blockchain.
 */
public class Block implements Serializable {

    private String hash;

    private String previousHash;

    private String data;

    private long timestamp;

    private int difficulty;

    private long nonce;

    public Block() {}

    public Block(String hash, String previousHash, String data,
                 long timestamp) {
        this.hash = hash;
        this.previousHash = previousHash;
        this.data = data;
        this.timestamp = timestamp;
    }

    public long getNonce() {
        return nonce;
    }

    public void setNonce(long nonce) {
        this.nonce = nonce;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public static Block fromString(String s){
        String[] attrs = s.split(",");
        if (attrs.length == 5) {
            String hash = attrs[0];
            String prevHash = attrs[1];
            String data = attrs[2];
            long nounce = Long.parseLong(attrs[3]);
            long timestamp = Long.parseLong(attrs[4]);
            Block result = new Block(hash, prevHash, data, timestamp);
            result.setDifficulty(20);
            result.setNonce(nounce);
            return result;
        }

        return null;
    }

}
