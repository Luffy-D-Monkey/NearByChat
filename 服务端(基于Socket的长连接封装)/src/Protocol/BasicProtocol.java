package Protocol;


import java.io.ByteArrayOutputStream;
import java.net.ProtocolException;

/*
数据ack类协议（DataAck）
    长度（length，32bit）
    版本号（version，8bit，前3位预留，后5位用于表示真正的版本号）
    数据类型（type，8bit，1表示数据ack）
    ack消息id（ackMsgId，32bit）
    预留信息（unused）
心跳类协议（ping）
    长度（length，32bit）
    版本号（version，8bit，前3位预留，后5位用于表示真正的版本号）
    数据类型（type，8bit，2表示心跳）
    心跳id（pingId，32bit，client上报取奇数，即1，3，5…，server下发取偶数，即0，2，4…）
    预留信息（unused）
心跳ack类协议（pingAck）
    长度（length，32bit）
    版本号（version，8bit，前3位预留，后5位用于表示真正的版本号）
    数据类型（type，8bit，3表示心跳ack）
    ack心跳id（pingId，32bit，client上报取奇数，即1，3，5…，server下发取偶数，即0，2，4…）
    预留信息（unused）


 */
public abstract class BasicProtocol
{

        // 长度均以字节（byte）为单位
        public static final int LENGTH_LEN = 4;       //记录整条数据长度数值的长度
        protected static final int VER_LEN = 1;       //协议的版本长度（其中前3位作为预留位，后5位作为版本号）
        protected static final int TYPE_LEN = 1;      //协议的数据类型长度

        private int reserved = 0;                     //预留信息
        private int version = Config.VERSION;         //版本号

        /**
         * 获取整条数据长度
         * 单位：字节（byte）
         *
         * @return
         */
        protected int getLength() {
            return LENGTH_LEN + VER_LEN + TYPE_LEN;
        }

        public int getReserved() {
            return reserved;
        }

        public void setReserved(int reserved) {
            this.reserved = reserved;
        }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * 获取协议类型，由子类实现
     *
     * @return
     */
    public abstract int getProtocolType();

    /**
     * 由预留值和版本号计算完整版本号的byte[]值
     *
     * @return
     */
    private int getVer(byte r, byte v, int vLen) {
        int num = 0;
        int rLen = 8 - vLen;
        for (int i = 0; i < rLen; i++) {
            num += (((r >> (rLen - 1 - i)) & 0x1) << (7 - i));
        }
        return num + v;
    }

    /**
     * 拼接发送数据，此处拼接了协议版本、协议类型和数据长度，具体内容子类中再拼接
     * 按顺序拼接
     *
     * @return
     */
    public byte[] genContentData() {
        byte[] length = SocketUtil.int2ByteArrays(getLength());
        byte reserved = (byte) getReserved();
        byte version = (byte) getVersion();
        byte[] ver = {(byte) getVer(reserved, version, 5)};
        byte[] type = {(byte) getProtocolType()};

        ByteArrayOutputStream baos = new ByteArrayOutputStream(LENGTH_LEN + VER_LEN + TYPE_LEN);
        baos.write(length, 0, LENGTH_LEN);
        baos.write(ver, 0, VER_LEN);
        baos.write(type, 0, TYPE_LEN);
        return baos.toByteArray();
    }
    /**
     * 解析出整条数据长度
     *
     * @param data
     * @return
     */
    protected int parseLength(byte[] data) {
        return SocketUtil.byteArrayToInt(data, 0, LENGTH_LEN);
    }

    /**
     * 解析出预留位
     *
     * @param data
     * @return
     */
    protected int parseReserved(byte[] data) {
        byte r = data[LENGTH_LEN];//前4个字节（0，1，2，3）为数据长度的int值，与版本号组成一个字节
        return (r >> 5) & 0xFF;
    }

    /**
     * 解析出版本号
     *
     * @param data
     * @return
     */
    protected int parseVersion(byte[] data) {
        byte v = data[LENGTH_LEN]; //与预留位组成一个字节
        return ((v << 3) & 0xFF) >> 3;
    }

    /**
     * 解析出协议类型
     *
     * @param data
     * @return
     */
    public static int parseType(byte[] data) {
        byte t = data[LENGTH_LEN + VER_LEN];//前4个字节（0，1，2，3）为数据长度的int值，以及ver占一个字节
        return t & 0xFF;
    }

    /**
     * 解析接收数据，此处解析了协议版本、协议类型和数据长度，具体内容子类中再解析
     *
     * @param data
     * @return
     * @throws ProtocolException 协议版本不一致，抛出异常
     */
    public int parseContentData(byte[] data) throws ProtocolException {
        int reserved = parseReserved(data);
        int version = parseVersion(data);
        int protocolType = parseType(data);
        if (version != getVersion()) {
            throw new ProtocolException("input version is error: " + version);
        }
        return LENGTH_LEN + VER_LEN + TYPE_LEN;
    }

    @Override
    public String toString() {
        return "Version: " + getVersion() + ", Type: " + getProtocolType();
    }


}
