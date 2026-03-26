package com.example.sobzybackend.dtos;

public class MessageResponse {
    private String message;
    private Boolean success;
    private int code;

    public MessageResponse() {}

    public MessageResponse(String message, Boolean success) {
        this.message = message;
        this.success = success;
    }

    public MessageResponse(String message, Boolean success, int code) {
        this.message = message;
        this.success = success;
        this.code = code;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public static MessageResponse success(String message) {
        return new MessageResponse(message, true);
    }

    public static MessageResponse error(String message) {
        return new MessageResponse(message, false);
    }

    public static MessageResponse error(String message, int code) {
        return new MessageResponse(message, false, code);
    }
}
