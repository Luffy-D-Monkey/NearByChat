package  Protocol;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;

public class PingAckProtocol extends BasicProtocol
{
    public static final int PROTOCOL_TYPE = 3;

    private static final int ACKPINGID_LEN = 4;

    private int ackPingId;

    private String unused;

    @Override
    public int getLength() {
        return super.getLength() + ACKPINGID_LEN + unused.getBytes().length;
    }

    @Override
    public int getProtocolType() {
        return PROTOCOL_TYPE;
    }

    public int getAckPingId() {
        return ackPingId;
    }

    public void setAckPingId(int ackPingId) {
        this.ackPingId = ackPingId;
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
        byte[] ackPingId = SocketUtil.int2ByteArrays(this.ackPingId);
        byte[] unused = this.unused.getBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(getLength());
        baos.write(base, 0, base.length);                //协议版本＋数据类型＋数据长度＋消息id
        baos.write(ackPingId, 0, ACKPINGID_LEN);         //消息id
        baos.write(unused, 0, unused.length);            //unused
        return baos.toByteArray();
    }
    @Override
    public int parseContentData(byte[] data) throws ProtocolException {
        int pos = super.parseContentData(data);

        //解析ackPingId
        ackPingId = SocketUtil.byteArrayToInt(data, pos, ACKPINGID_LEN);
        pos += ACKPINGID_LEN;

        //解析unused
        try {
            unused = new String(data, pos, data.length - pos, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return pos;
    }
}
