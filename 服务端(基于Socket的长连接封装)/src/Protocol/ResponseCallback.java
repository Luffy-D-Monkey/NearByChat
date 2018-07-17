package Protocol;

public interface ResponseCallback {

    void targetIsOffline(DataProtocol reciveMsg);

    void targetIsOnline(String clientIp);
}