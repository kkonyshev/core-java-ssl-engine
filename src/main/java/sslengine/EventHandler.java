package sslengine;

public abstract class EventHandler {
    abstract void onSuccessHandler();
    abstract void onErrorHandler(Exception e);
}
