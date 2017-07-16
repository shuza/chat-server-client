import java.io.Serializable;

/**
 * Created by Boka on 17-Jul-17.
 */
public class ChatMessage implements Serializable {

    protected static final long serialVersionUID = 111L;

    // type of action
    static final int USERS = 0, MESSAGE = 1, LOGOUT = 2, ACK = 3,
            PACKET_LENGTH = 4, ERROR_CODE = 5, COMPLETE = 6;

    private int type, destenatioinId, sourceId;
    private String message;
    private boolean isACK;
    private int ACK_TYPE; // 1 for msg length, 2 for error
    private int ACK_NO; // for 1 its msg length.. for 2 its number of missing
    // packet

    public ChatMessage(int type, String message) {
        this(type, message, 0);
    }

    public ChatMessage(int type, String message, int destenationId) {
        this.type = type;
        this.message = message;
        this.destenatioinId = destenationId;
    }

    public ChatMessage(int type, int sourceId, int destenationId) {
        this.type = type;
        this.sourceId = sourceId;
        this.destenatioinId = destenationId;
    }

    public void enableACK() {
        isACK = true;
    }

    public boolean getACK() {
        return isACK;
    }

    public void setAckType(int ackType) {
        this.ACK_TYPE = ackType;
    }

    public int getAckType() {
        return ACK_TYPE;
    }

    public void setAckNo(int ackNo) {
        this.ACK_NO = ackNo;
    }

    public int getAckNo() {
        return ACK_NO;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    public String getMessage() {
        return message;
    }

    public int getSourceId(){
        return sourceId;
    }

    public int getDestenationId() {
        return destenatioinId;
    }

}
