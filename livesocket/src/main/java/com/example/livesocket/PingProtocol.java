package com.example.livesocket;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;

public class PingProtocol extends BasicProtocol {
    public static final int PROTOCOL_TYPE = 2;

    private static final int PINGID_LEN = 4;

    private int pingId;

    private String unused;

    @Override
    public int getLength() {
        return super.getLength() + PINGID_LEN + unused.getBytes().length;
    }

    @Override
    public int getProtocolType() {
        return PROTOCOL_TYPE;
    }

    public int getPingId() {
        return pingId;
    }

    public void setPingId(int pingId) {
        this.pingId = pingId;
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
        byte[] pingId = SocketUtil.int2ByteArrays(this.pingId);
        byte[] unused = this.unused.getBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(getLength());
        baos.write(base, 0, base.length);          //协议版本＋数据类型＋数据长度＋消息id
        baos.write(pingId, 0, PINGID_LEN);         //消息id
        baos.write(unused, 0, unused.length);            //unused
        return baos.toByteArray();
    }

    @Override
    public int parseContentData(byte[] data) throws ProtocolException {
        int pos = super.parseContentData(data);

        //解析pingId
        pingId = SocketUtil.byteArrayToInt(data, pos, PINGID_LEN);
        pos += PINGID_LEN;

        try {
            unused = new String(data, pos, data.length - pos, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return pos;
    }
}
