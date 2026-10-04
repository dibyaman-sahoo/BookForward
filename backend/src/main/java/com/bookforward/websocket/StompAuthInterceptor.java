package com.bookforward.websocket;

import com.bookforward.security.TokenAuthenticator;
import com.bookforward.security.UserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/** Authenticates CONNECT frames with the JWT and authorises every SUBSCRIBE/SEND destination server-side. */
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {
    private final TokenAuthenticator authenticator;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc == null || acc.getCommand() == null) return message;
        StompCommand cmd = acc.getCommand();

        if (StompCommand.CONNECT.equals(cmd)) {
            List<String> auth = acc.getNativeHeader("Authorization");
            String header = (auth == null || auth.isEmpty()) ? null : auth.get(0);
            if (header == null || !header.startsWith("Bearer ")) throw new MessagingException("Authentication required");
            UserPrincipal p = authenticator.authenticate(header.substring(7)).orElseThrow(() -> new MessagingException("Invalid or expired token"));
            acc.setUser(new UsernamePasswordAuthenticationToken(p, null, p.authorities()));
            return message;
        }
        if (acc.getUser() == null && !StompCommand.DISCONNECT.equals(cmd)) {
            throw new MessagingException("Not authenticated");
        }
        String dest = acc.getDestination();
        if (StompCommand.SUBSCRIBE.equals(cmd)) {
            boolean ok = dest != null && (dest.equals("/topic/presence") || dest.startsWith("/user/queue/"));
            if (!ok) throw new MessagingException("Subscription not permitted");
        } else if (StompCommand.SEND.equals(cmd)) {
            boolean ok = "/app/chat.send".equals(dest) || "/app/chat.typing".equals(dest);
            if (!ok) throw new MessagingException("Destination not permitted");
        }
        return message;
    }
}
