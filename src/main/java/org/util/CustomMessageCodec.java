package org.util;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.dto.Url;

public class CustomMessageCodec implements MessageCodec<Message, Message> {

    @Override
    public void encodeToWire(Buffer buffer, Message message) {
        JsonObject jsonToEncode = new JsonObject();
        jsonToEncode.put("statusCode", message.getStatusCode());
        jsonToEncode.put("message", message.getMessage());
        jsonToEncode.put("url", new JsonObject(Json.encode(message.getUrl())));
        jsonToEncode.put("httpMethod", message.getHttpMethod());
        jsonToEncode.put("resource", message.getResource());
        jsonToEncode.put("pathParameters", message.getPathParams());

        // Encode object to string
        String jsonToStr = jsonToEncode.encode();

        // Length of JSON: is NOT characters count
        int length = jsonToStr.getBytes().length;

        // Write data into given buffer
        buffer.appendInt(length);
        buffer.appendString(jsonToStr);
    }

    @Override
    public Message decodeFromWire(int i, Buffer buffer) {
        int _pos = i;

        // Length of JSON
        int length = buffer.getInt(_pos);

        // Get JSON string by it`s length
        // Jump 4 because getInt() == 4 bytes
        String jsonStr = buffer.getString(_pos+=4, _pos+=length);
        JsonObject contentJson = new JsonObject(jsonStr);

        // Get fields
        int statusCode = contentJson.getInteger("statusCode");
        Url url = contentJson.getJsonObject("url").mapTo(Url.class);
        String message = contentJson.getString("message");
        String httpMethod = contentJson.getString("httpMethod");
        String resource = contentJson.getString("resource");
        String pathParams = contentJson.getString("pathParameters");

        // We can finally create custom message object
        return new Message(statusCode, message, httpMethod, resource, pathParams, url);
    }

    @Override
    public Message transform(Message message) {
        return message;
    }

    @Override
    public String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
