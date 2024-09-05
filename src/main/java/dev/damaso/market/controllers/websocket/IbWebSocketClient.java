package dev.damaso.market.controllers.websocket;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.TickleResult;
import dev.damaso.market.operations.Date;

@Component
public class IbWebSocketClient implements DisposableBean { // implements InitializingBean {
    private static Logger logger = LogManager.getLogger(IbWebSocketClient.class);

    @Value("${ibgw.ws.baseurl}")
    private String baseUrl;

    @Value("${jenkins.baseurl}")
    private String jenkinsUrl;

    @Value("${jenkins.username}")
    private String jenkinsUsername;

    @Value("${jenkins.password}")
    private String jenkinsPassword;

    @Autowired
    public Api api;

    @Autowired
    public ObjectMapper objectMapper;

    @Autowired
    RestTemplate restTemplate;

    public WebSocketSession webSocketSession;

    public boolean subscribed;

    public LocalDateTime connectedDateTime;

    public void ensure() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        if (Date.isNasdaqExtendedOpen(now.toLocalDateTime())) {
            if (webSocketSession == null || !webSocketSession.isOpen()) {
                logger.info("reconnect");
                connect();
            }
            if (webSocketSession != null && webSocketSession.isOpen() && connectedDateTime!=null && !subscribed) {
                LocalDateTime ldNow = LocalDateTime.now();
                LocalDateTime expectedConnectedDateTime = connectedDateTime.plus(20, ChronoUnit.SECONDS);
                if (ldNow.isBefore(expectedConnectedDateTime)) {
                    logger.info("subscription pending");
                }
            }
        }
    }

    public void connect() {
        String url = "%s/v1/api/ws".formatted(baseUrl);

        logger.info("connect: " + url);
        try {
            StandardWebSocketClient webSocketClient = new StandardWebSocketClient();

            TrustManager[] trustAllCerts = new TrustManager[] {new MyTrustManager() };       
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(new KeyManager[0], trustAllCerts, new java.security.SecureRandom());
 
            Map<String, Object> properties = new HashMap<>();
            properties.put("org.apache.tomcat.websocket.SSL_CONTEXT", sc);
            webSocketClient.setUserProperties(properties);

            WebSocketSession webSocketSession = webSocketClient.doHandshake(new AbstractWebSocketHandler() {
                 @Override
                public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
                    // https://interactivebrokers.github.io/cpwebapi/websockets
                    String val = new String(message.getPayload().array());
                    JsonNode jsonNode = objectMapper.readTree(val);
                    boolean authenticated = true;
                    if (jsonNode.has("topic")) {
                        String topic = jsonNode.get("topic").asText();
                        // logger.info("topic: " + topic);
                        if (topic.equals("sts")) {
                            logger.info("received binary message: " + val);
                            // {"topic":"sts","args":{"authenticated":false}}
                            JsonNode args = jsonNode.get("args");
                            if (args.has("authenticated")) {
                                authenticated = jsonNode.get("args").get("authenticated").asBoolean();
                                if (authenticated) {
                                    subscribe(session);
                                    updateOrder("");
                                }
                            }
                        } else if (topic.equals("sor") && !jsonNode.has("error")) {
                            // logger.info("received binary message: " + val);
                            JsonNode argsNode = jsonNode.get("args");
                            for (JsonNode argNode : argsNode) {
                                String status = "PartialFill";
                                if (argNode.has("status")) {
                                    status = argNode.get("status").asText();
                                }
                                if (status.equals("PartialFill") || status.equals("Filled")) {
                                    double filledQuantity = 0.0;
                                    if (argNode.has("filledQuantity")) {
                                        filledQuantity = argNode.get("filledQuantity").asDouble();
                                    }
                                    if (filledQuantity > 0) {
                                        logger.info("order args: " + val);
                                        double remainingQuantity = 0.0;
                                        if (argNode.has("remainingQuantity")) {
                                            remainingQuantity = argNode.get("remainingQuantity").asDouble();
                                        }
                                        String side = "UNKNOWN";
                                        if (argNode.has("side")) {
                                            side = argNode.get("side").asText();
                                        }
                                        logger.info("order trade %s %s %.2f %.2f".formatted(side, status, remainingQuantity, filledQuantity));
                                        if (argNode.has("orderId")) {
                                            String orderId = argNode.get("orderId").asText();
                                            updateOrder(orderId);
                                        }
                                    }
                                }
                            }
                        } else if (topic.equals("sor") && jsonNode.has("error")) {
                            subscribed = false;
                            // This error occurs after subscribing when non-authenticated (after logout or authentication expired)
                            logger.info("reauthenticate due to sor error");
                            api.reauthenticateHelper();
                            subscribe(session);
                        } else if (topic.equals("system")) {
                            // pass
                            if (jsonNode.has("success")) {
                               logger.info("success");
                               subscribe(session);
                            }
                            // logger.info("received binary message: " + val);
                        } else {
                            logger.info("received binary message: " + val);
                        }
                    } else if (jsonNode.has("message")) {
                        String txtMessage = jsonNode.get("message").asText();
                        if (txtMessage.equals("waiting for session")) {
                            subscribed = false;
                            logger.info("sending session...");
                            setSession(session);
                            subscribe(session);
                        }
                    } else {
                        logger.info("received binary message: " + val);
                    }
                    if (!authenticated) {
                        subscribed = false;
                        logger.info("closing connection...");
                        session.close();
                    }
                }

                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    logger.info("established connection");
                    connectedDateTime = LocalDateTime.now();
                    // try {
                    //     logger.info("is open: " + session.isOpen());
                    //     // setSession(session);
                    //     // subscribe(session);
                    //     // logger.info("Message sent: " + text.getPayload());
                    // } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized ex) {
                    //     logger.info("unauthorized");
                    //     try {
                    //         session.close();
                    //     } catch (Exception ex2) {
                    //         ex2.printStackTrace();
                    //     }
                    // } catch (Exception ex) {
                    //     ex.printStackTrace();
                    // }
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                    logger.info("websocket closed");
                    connectedDateTime = null;
                }

                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                    logger.info("handleTransportError");
                    exception.printStackTrace();
                }

            }, new WebSocketHttpHeaders(), URI.create(url)).get();
            this.webSocketSession = webSocketSession;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setSession(WebSocketSession session) throws IOException {
        TickleResult tickle = api.tickle();
        String sessionMesssage = "{\"session\":\"%s\"}".formatted(tickle.session);
        TextMessage text = new TextMessage(sessionMesssage);
        session.sendMessage(text);
    }

    public void subscribe(WebSocketSession session) throws IOException {
        String subs = "sor+{}";
        session.sendMessage(new TextMessage(subs));
        logger.info("subscribed");
        subscribed = true;
    }

    @Override
    public void destroy() throws Exception {
        if (webSocketSession != null && webSocketSession.isOpen()) {
            logger.info("close socket on disposal");
            webSocketSession.close();
        }
    }

    public boolean getSubscribed() {
        return this.subscribed;
    }

    public void updateOrder(String orderId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String auth = jenkinsUsername + ":" + jenkinsPassword;
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
            String authHeader = "Basic " + new String( encodedAuth );
            headers.set( "Authorization", authHeader );
            String url = "%s/job/ib-order-update/buildWithParameters?token=fastbuy&orderId=%s".formatted(jenkinsUrl, orderId);
            logger.info(url);
            restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Object>(headers), Void.class);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
}

