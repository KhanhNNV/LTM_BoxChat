// TẠO FILE MỚI: src/main/java/chatapp/controller/BaseController.java
package chatapp.controller;

import chatapp.model.Client;
import chatapp.model.NetworkMessage;

public abstract class BaseController {

    public void initializeController() {
        System.out.println("[DEBUG/BaseController] Initializing controller: " + this.getClass().getName()); // DEBUG
        System.out.println("[DEBUG/BaseController] Setting message handler for the client."); // DEBUG
        // Đăng ký lắng nghe tin nhắn từ server
        Client.getInstance().setOnMessageReceived(this::handleServerMessage);
    }

    // Các controller con phải override phương thức này để xử lý tin nhắn
    protected abstract void handleServerMessage(NetworkMessage message);
}
