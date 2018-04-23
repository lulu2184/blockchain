import lib.Block;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BlockChain implements BlockChainBase{
    private static String genesisData = "GENESIS";
    private static String genesisHash = "10101010";

    private int difficulty = 20;
    private int nodeId;
    private List<Block> blockChain;

    public BlockChain(int difficulty, int nodeId) {
        this.difficulty = difficulty;
        this.nodeId = nodeId;
        blockChain = new ArrayList<>();
        blockChain.add(createGenesisBlock());
    }

    @Override
    public boolean addBlock(Block block) {
        return false;
    }

    @Override
    public Block createGenesisBlock() {
        return new Block(genesisHash, genesisHash, genesisData, (new Date()).getTime());
    }

    @Override
    public byte[] createNewBlock(String data) {
        return new byte[0];
    }

    @Override
    public boolean broadcastNewBlock() {
        return false;
    }

    @Override
    public void setDifficulty(int difficulty) {

    }

    @Override
    public byte[] getBlockchainData() {
        return new byte[0];
    }

    @Override
    public void downloadBlockchain() {

    }

    @Override
    public void setNode(Node node) {

    }

    @Override
    public boolean isValidNewBlock(Block newBlock, Block prevBlock) {
        return false;
    }

    @Override
    public Block getLastBlock() {
        if (blockChain.size() > 0)
            return blockChain.get(blockChain.size() - 1);
        else
            return null;
    }

    @Override
    public int getBlockChainLength() {
        return blockChain.size();
    }
}
