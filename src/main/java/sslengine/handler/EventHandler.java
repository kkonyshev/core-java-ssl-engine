package sslengine.handler;

public abstract class EventHandler {
    public abstract void onSuccessHandler();
    public abstract void onErrorHandler(Exception e);
}
