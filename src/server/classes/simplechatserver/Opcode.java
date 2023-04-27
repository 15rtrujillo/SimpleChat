package simplechatserver;

import java.util.HashMap;

/**
 *
 * @author RYTRUJILLO
 */
public enum Opcode {
    SET_USERNAME(0),
    CONNECTION_ACCEPTED(1),
    CONNECTION_DENIED(2),
    SEND_CHAT_MESSAGE(3),
    SEND_PRIVATE_MESSAGE(4);
    
    private final byte opcode;
    
    private static final HashMap<Byte, Opcode> byId = new HashMap<Byte, Opcode>();
    
    static {
        for (Opcode opcode : Opcode.values()) {
            byId.put(opcode.id(), opcode); 
        }
    }
    
    Opcode(int opcode) {
        this.opcode = (byte)opcode;
    }
    
    public byte id() {
        return this.opcode;
    }
    
    public static Opcode getOpcodeById(int opcode) {
        return byId.get((byte)opcode);
    }
}
