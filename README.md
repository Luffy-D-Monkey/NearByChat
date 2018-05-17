    最近想做一个聊天的app软件，凑巧在社区上看到这个demo，地址https://github.com/EzraLopez/nearbychat
	于是想模仿一下，发现在国内用不了，可能用法不对还是什么的
于是就想修改一下，基于位置的聊天应用。
位置保存的信息是GeoHash，引用了https://github.com/drfonfon/android-geohash

与服务器通信方式采用socket长连接（心跳），参考大佬demo，地址 https://github.com/HouBin506/SocketPushClient

    运行Socket demo发现，client在发送消息的时候会出现android.os.NetworkOnMainThreadException，究其原因是作者处理发送消息的时候，在主线程中执行了Socket IO 操作，这个Exception在android6.0的时候，google没有进行处理，也就是说Socket demo在android6.0的手机上可以运行，7.0则不行。在android7.0的时候得到改正。所以这里的处理方法就是把发送消息那里的IO操作放到了一个简单的子线程中处理。

