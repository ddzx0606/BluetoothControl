package vimerzhao.bluetoothcontrol;


public class ChatMsgEntity {
    private static final String TAG = ChatMsgEntity.class.getSimpleName();

    private String name;

    private String date;

    private String text;

    private boolean msgType = true;

    public boolean getMsgType() {
        return msgType;
    }

    public void setMsgType(boolean msgType) {
        this.msgType = msgType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }



    public ChatMsgEntity() {
    }

    public ChatMsgEntity(String name, String date, String text, boolean msgType) {
        this.name = name;
        this.date = date;
        this.text = text;
        this.msgType = msgType;
    }
}
