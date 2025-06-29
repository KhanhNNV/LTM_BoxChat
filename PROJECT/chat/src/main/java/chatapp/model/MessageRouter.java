// src/main/java/chatapp/model/MessageRouter.java
package chatapp.model;

import java.util.function.Consumer;

public class MessageRouter {
    private static Consumer<NetworkMessage> currentHandler;

    public static void setHandler(Consumer<NetworkMessage> handler) {
        currentHandler = handler;
        System.out.println("[DEBUG] MessageRouter setHandler: " + handler);
    }

    public static void route(NetworkMessage message) {
        if (currentHandler != null) {
            currentHandler.accept(message);
        }
    }
}

