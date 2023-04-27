package simplechatserver;

/**
 *
 * @author RYTRUJILLO
 */
public class Packet {
    private byte opcode;
    private byte[] data;
    
    public Packet(byte opcode) {
        this.opcode = opcode;
        this.data = new byte[0];
    }
    
    public Packet(byte opcode, byte[] data) {
        this.opcode = opcode;
        this.data = data;
    }
    
    public byte getOpcode() {
        return opcode;
    }
    
    public byte[] getData() {
        return data;
    }
}
