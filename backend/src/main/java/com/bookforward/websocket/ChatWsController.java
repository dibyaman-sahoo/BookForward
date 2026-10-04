package com.bookforward.websocket;

import com.bookforward.dto.ChatDtos.*;
import com.bookforward.exception.ApiException;
import com.bookforward.service.ChatService;
import com.bookforward.entity.MessageType;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWsController {
    private final ChatService chat;

    @MessageMapping("/chat.send")
    public void send(@Payload WsSendMessage payload, Principal principal) {
        if (payload == null || payload.conversationId() == null) throw ApiException.badRequest("INVALID_MESSAGE", "conversationId is required");
        chat.post(payload.conversationId(), UUID.fromString(principal.getName()), payload.content(), MessageType.TEXT);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload WsTyping payload, Principal principal) {
        if (payload != null && payload.conversationId() != null) {
            chat.typing(payload.conversationId(), UUID.fromString(principal.getName()));
        }
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public Map<String, String> onError(Exception e) {
        if (e instanceof ApiException a) return Map.of("code", a.getCode(), "message", a.getMessage());
        log.warn("WebSocket handler error: {}", e.toString());
        return Map.of("code", "INTERNAL_ERROR", "message", "Message could not be processed");
    }
}
