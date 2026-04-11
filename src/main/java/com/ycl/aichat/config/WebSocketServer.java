package com.ycl.aichat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@ServerEndpoint("/websocket/{sid}")
@Slf4j
public class WebSocketServer {
    private static int onlineCount = 0;
    private static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet();
    private Session session;
    private String sid = "";

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        this.session = session;
        this.sid = sid;
        webSocketSet.add(this);
        addOnlineCount();
        log.info("有新窗口开始监听,当前在线人数为" + getOnlineCount());
    }

    @OnClose
    public void onClose() {
        webSocketSet.remove(this);
        subOnlineCount();
        log.info("有一连接关闭！当前在线人数为" + getOnlineCount());
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("收到客户端消息：" + message);
        for (WebSocketServer webSocketServer : webSocketSet) {
            webSocketServer.sendMessage(message);
        }
    }

    @OnError
    public void onError(Throwable throwable) {
        log.error(throwable.getMessage());
    }

    public void sendMessage(String message) {
        try {
            this.session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.error("推送失败");
        }
    }

    public static void sendInfo(String message, @PathParam("sid") String userId) throws IOException {
        for (WebSocketServer item : webSocketSet) {
            if ("all".equals(userId)) {
                log.info("推送消息到所有窗口，推送内容:" + message);
                for (WebSocketServer webSocketServer : webSocketSet) {
                    webSocketServer.sendMessage(message);
                }
            } else if (item.sid.equals(userId)) {
                item.sendMessage(message);
                log.info("推送消息到窗口" + userId + "，推送内容:" + message);
            }
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }
}