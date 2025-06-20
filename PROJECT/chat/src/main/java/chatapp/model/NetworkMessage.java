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
        BACK_HOME_REQUEST,
        BACK_HOME_SUCCESS,
        SEND_MESSAGE_REQUEST,
        GET_JOINED_GROUPS_REQUEST,
        GET_ROOM_HISTORY_REQUEST,
        GET_MEMBERS_GROUP_REQUEST,
        GET_USER_REQUEST,
        JOIN_EXISTING_ROOM_REQUEST,
        SEND_FILES_REQUEST,
        REMOVE_MEMBER_REQUEST,
        // Client gửi yêu cầu tham gia phòng đã tham gia trước đó

        // Server to Client Responses/Broadcasts
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        REGISTER_SUCCESS,
        REGISTER_FAILURE,
        ROOM_CREATED,
        ROOM_JOINED,
        ROOM_JOIN_FAILED,
        ROOM_DELETED,
        RECEIVE_MESSAGE, // Tin nhắn từ người khác
        USER_JOINED_ROOM,
        USER_LEFT_ROOM,
        ERROR_RESPONSE,
        JOINED_GROUPS_RESPONSE,
        ROOM_HISTORY_RESPONSE,
        MEMBERS_GROUP_RESPONSE,
        USER_RESPONSE,
        JOIN_EXISTING_ROOM_RESPONSE,
        RECEIVE_FILES, // Server phản hồi yêu cầu tham gia phòng đã tham gia
        FILE_TRANSFER_ERROR,
        MEMBER_REMOVED_SUCCESS, // Gửi cho leader để xác nhận
        MEMBER_LIST_UPDATE, // Gửi cho cả phòng để cập nhật danh sách
        YOU_HAVE_BEEN_REMOVED // Gửi riêng cho người bị xóa
    }
}