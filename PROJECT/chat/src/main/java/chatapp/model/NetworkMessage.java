package chatapp.model;

import java.io.Serializable;

public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L; // Để đảm bảo tương thích khi serialize (tuần tự hóa)

    private final MessageType type;
    private final Object payload; // Dữ liệu đi kèm (User, Room, String, etc.)

    public NetworkMessage(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    // Enum định nghĩa các loại yêu cầu/phản hồi
    public enum MessageType {
        // Client to Server Requests
        LOGIN_REQUEST,
        REGISTER_REQUEST,
        CREATE_ROOM_REQUEST,
        JOIN_ROOM_REQUEST,
        LEAVE_ROOM_REQUEST,
        SEND_MESSAGE_REQUEST,
        GET_JOINED_GROUPS_REQUEST,
        GET_ROOM_HISTORY_REQUEST,
        GET_MEMBERS_GROUP_REQUEST,
        GET_USER_REQUEST,
        JOIN_EXISTING_ROOM_REQUEST,
        SEND_FILES_REQUEST,// Client gửi yêu cầu tham gia phòng đã tham gia trước đó


        // Server to Client Responses/Broadcasts
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        REGISTER_SUCCESS,
        REGISTER_FAILURE,
        ROOM_CREATED,
        ROOM_JOINED,
        ROOM_JOIN_FAILED,
        RECEIVE_MESSAGE, // Tin nhắn từ người khác
        USER_JOINED_ROOM,
        USER_LEFT_ROOM,
        ERROR_RESPONSE,
        JOINED_GROUPS_RESPONSE,
        ROOM_HISTORY_RESPONSE,
        MEMBERS_GROUP_RESPONSE,
        USER_RESPONSE,
        JOIN_EXISTING_ROOM_RESPONSE,
        RECEIVE_FILES,// Server phản hồi yêu cầu tham gia phòng đã tham gia
        FILE_TRANSFER_ERROR,
        CHANGE_PASSWORD_REQUEST,
        CHANGE_PASSWORD_FAILURE,
        CHANGE_PASSWORD_SUCCESS,
        UPDATE_FULLNAME_FAILURE,
        UPDATE_FULLNAME_SUCCESS,
        UPDATE_GMAIL_FAILURE,
        UPDATE_GMAIL_SUCCESS,
        UPDATE_GMAIL_REQUEST,
        UPDATE_ROOM_NAME_SUCCESS,
        UPDATE_ROOM_NAME_FAILURE,
        UPDATE_ROOM_NAME_REQUEST,
        UPDATE_ROOM_PASSWORD_SUCCESS,
        UPDATE_ROOM_PASSWORD_FAILURE,
        UPDATE_ROOM_PASSWORD_REQUEST,
        SEARCH_ROOM_REQUEST,
        SEARCH_ROOM_RESPONSE,
        UPDATE_FULLNAME_REQUEST
        /// them code sua thong tin
    }
}