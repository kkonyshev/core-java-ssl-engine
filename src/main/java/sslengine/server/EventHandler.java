package sslengine.server;

public abstract class EventHandler {
    public abstract void onSuccessHandler();
    public abstract void onErrorHandler(Exception e);
}
