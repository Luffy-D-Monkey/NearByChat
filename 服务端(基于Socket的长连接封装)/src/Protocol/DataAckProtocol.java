package  Protocol;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;

public class DataAckProtocol extends BasicProtocol {
    public static final int PROTOCOL_TYPE = 1;

    private static final int ACKMSGID_LEN = 4;

    private int ackMsgId;

    private String unused;

    @Override
    public int getLength() {
        return super.getLength() + ACKMSGID_LEN + unused.getBytes().length;
    }

    @Override
    public int getProtocolType() {
        return PROTOCOL_TYPE;
    }

    public int getAckMsgId() {
        return ackMsgId;
    }

    public void setAckMsgId(int ackMsgId) {
        this.ackMsgId = ackMsgId;
    }
    public String getUnused() {
        return unused;
    }

    public void setUnused(String unused) {
        this.unused = unused;
    }

    /**
     * 拼接发送数据
     *
     * @return
     */
    @Override
    public byte[] genContentData() {
        byte[] base = super.genContentData();
        byte[] ackMsgId = SocketUtil.int2ByteArrays(this.ackMsgId);
        byte[] unused = this.unused.getBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(getLength());
        baos.write(base, 0, base.length);              //协议版本＋数据类型＋数据长度＋消息id
        baos.write(ackMsgId, 0, ACKMSGID_LEN);         //消息id
        baos.write(unused, 0, unused.length);          //unused
        return baos.toByteArray();
    }

    @Override
    public int parseContentData(byte[] data) throws ProtocolException {
        int pos = super.parseContentData(data);

        //解析ackMsgId
        ackMsgId = SocketUtil.byteArrayToInt(data, pos, ACKMSGID_LEN);
        pos += ACKMSGID_LEN;

        //解析unused
        try {
            unused = new String(data, pos, data.length - pos, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return pos;
    }
}
