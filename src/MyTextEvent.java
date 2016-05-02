import java.io.Serializable;

/**
 * @author Jesper Buus Nielsen
 */
public class MyTextEvent implements Serializable {
    private int offset;
    private int timestamp;

    protected MyTextEvent(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    public int getTimestamp(){
        return this.timestamp;
    }

    public void setTimestamp(int timestamp){
         this.timestamp = timestamp;
    }
}
