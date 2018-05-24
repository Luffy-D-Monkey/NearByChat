    最近想做一个聊天的app软件，凑巧在社区上看到这个demo，地址https://github.com/EzraLopez/nearbychat
	于是想模仿一下，发现在国内用不了，可能用法不对还是什么的
于是就想修改一下，基于位置的聊天应用。
位置保存的信息是GeoHash，引用了https://github.com/drfonfon/android-geohash

与服务器通信方式采用socket长连接（心跳），参考大佬Socket demo，地址 https://github.com/HouBin506/SocketPushClient

    运行Socket demo发现，client在发送消息的时候会出现android.os.NetworkOnMainThreadException，究其原因是作者处理发送消息的时候，在主线程中执行了Socket IO 操作，这个Exception在android6.0的时候，google没有进行处理，也就是说Socket demo在android6.0的手机上可以运行，7.0则不行。在android7.0的时候得到改正。所以这里的处理方法就是把发送消息那里的IO操作放到了一个简单的子线程中处理。
次日，正式放弃上述Socket demo（2018-05-19）

基于枚杉博客 https://blog.csdn.net/u010818425/article/details/53448817 实现socket的长连接（心跳）（2018-05-24），在他的博客中的代码，由于在根据收到消息解析成不同的Protocol的时候，需要根据工程中的路径进行类加载，所以如果是自己实现的话，就需要自己修改SocketUtil类中加载类的路径。而在本Android studio的demo中，直接引用livesocket的话，客户端的加载类路径则不需要修改，可以直接使用。谨记客户端和服务端的SocketUtil中的加载类路径都可能需要修改。


