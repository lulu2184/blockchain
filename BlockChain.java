import lib.Block;
import lib.Message;
import lib.MessageType;
import lib.TransportLib;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BlockChain implements BlockChainBase{
    private static String genesisData = "GENESIS";
    private static String genesisHash = "10101010";

    private int difficulty;
    private int nodeId;
    private int num_peers;
    private List<Block> blockChain;
    private TransportLib lib;
    private Block newBlock;
    private Node node;

    public BlockChain(int difficulty, int nodeId, int num_peers, TransportLib lib, Node node) {
        this.difficulty = difficulty;
        this.nodeId = nodeId;
        this.num_peers = num_peers;
        this.lib = lib;
        this.node = node;
        blockChain = new ArrayList<>();
        blockChain.add(createGenesisBlock());
    }

    @Override
    public boolean addBlock(Block block) {
        int length = blockChain.size();
        for (int i = length-1; i >= 0; --i) {
            Block prevBlock = blockChain.get(i);
            if (prevBlock.getTimestamp() > block.getTimestamp()) {
                return false;
            }
            if (isValidNewBlock(block, prevBlock)) {
                if (i != length-1) {
                    if (blockChain.get(i+1).getTimestamp() > block.getTimestamp()) {
                        for (int j = length-1; j >= i+1; --j)
                            blockChain.remove(j);
                    } else {
                        return false;
                    }
                }
                blockChain.add(block);
                return true;
            }
        }
        return false;
    }



    @Override
    public Block createGenesisBlock() {
        return new Block(genesisHash, genesisHash, genesisData, (new Date()).getTime());
    }

    @Override
    public byte[] createNewBlock(String data) {
        String prev_hash = blockChain.get(blockChain.size()-1).getHash();
        long nonce = 0;
        newBlock = new Block("", prev_hash, data, (new Date()).getTime());
        newBlock.setNonce(nonce);
        newBlock.setDifficulty(difficulty);
        String hash = "";
        while(nonce < Long.MAX_VALUE && !hash.startsWith(getStarter(newBlock))) {
            nonce ++;
            newBlock.setNonce(nonce);
            hash = calculateBlockHash(newBlock);
            newBlock.setHash(hash);
        }
        newBlock.setTimestamp(new Date().getTime());
        return convertBlockToByteArray(newBlock);
    }

    @Override
    public boolean broadcastNewBlock() {
        SuccessListener successListener = new SuccessListener();
        int peers_num = node.getPeerNumber();
        for (int i = 0; i < peers_num; ++i) {
            if (i != nodeId) {
                BroadcastNewBlockThread thread = new BroadcastNewBlockThread(i, successListener);
                (new Thread(thread)).start();
            }
        }

        while (!successListener.isFinished());
        return successListener.isCommitted;
    }

    @Override
    public void setDifficulty(int difficulty) {

    }

    @Override
    public byte[] getBlockchainData() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(blockChain);
            byte[] bytes = bos.toByteArray();
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public void downloadBlockchain() {
        int peer_num = node.getPeerNumber();
        for (int i = 0; i < peer_num; ++i) {
            if (i != nodeId) {
                try {
                    byte[] bytes = node.getBlockChainDataFromPeer(i);
                    ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
                    Object object = null;
                    try {
                        ObjectInputStream inputStream = new ObjectInputStream(byteStream);
                        object = inputStream.readObject();
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                    List<Block> chain = (List<Block>)object;
                    if (blockChain.size() != 0) {
                        if (chain.size() > blockChain.size()){
                            blockChain = chain;
                        } else if (chain.size() == blockChain.size()) {
                            if (chain.get(chain.size()-1).getTimestamp() < blockChain.get(blockChain.size()-1).getTimestamp())
                                blockChain = chain;
                        }
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public boolean isValidNewBlock(Block newBlock, Block prevBlock) {
        return newBlock.getPreviousHash().equals(prevBlock.getHash())
                && calculateBlockHash(newBlock).startsWith(getStarter(newBlock));
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

    private String calculateBlockHash(Block block) {
        String originalStr = block.getPreviousHash() + block.getData() + block.getNonce();
        String hash = DigestUtils.sha256Hex(originalStr);
        return hash;
    }

    private String getStarter(Block block) {
        int times = block.getDifficulty()/4;
        String starter = "";
        for (int i = 0; i < times; ++i) {
            starter += "0";
        }
        return starter;
    }

    private static byte[] convertBlockToByteArray(Block block) {
        String result = block.getHash() + "," + block.getPreviousHash() + ","
                + block.getData() + "," + block.getNonce() + "," + block.getTimestamp();
        return result.getBytes();
    }

    private class BroadcastNewBlockThread implements Runnable {
        private int peerId;
        private SuccessListener successListener;

        private BroadcastNewBlockThread(int peerId, SuccessListener successListener) {
            this.peerId  = peerId;
            this.successListener = successListener;
        }

        @Override
        public void run() {
            try {
                boolean success = node.broadcastNewBlockToPeer(peerId, convertBlockToByteArray(newBlock));
                if (success) {
                    successListener.onSuccess();
                } else {
                    successListener.onFailure();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private class SuccessListener {
        private int successCounter;
        private boolean isCommitted;
        private int replyConnter;

        private SuccessListener() {
            this.isCommitted = false;
            this.successCounter = 1;
            this.replyConnter = 1;
        }

        private void onSuccess() {
            synchronized (this) {
                successCounter++;
                replyConnter++;
                if (successCounter == num_peers) {
                    blockChain.add(newBlock);
                    isCommitted = true;
                }
            }
        }

        private void onFailure() {
            synchronized (this) {
                replyConnter++;
            }
        }

        private Boolean isCommitted() {
            synchronized (this) {
                return isCommitted;
            }
        }

        private Boolean isFinished() {
            synchronized (this) {
                return replyConnter == num_peers;
            }
        }
    }
}
