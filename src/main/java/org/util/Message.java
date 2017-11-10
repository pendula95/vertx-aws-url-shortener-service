package org.util;

import org.dto.Url;

public class Message {

    private int statusCode;
    private String message;
    private String httpMethod;
    private String pathParams;
    private String resource;
    private Url url;

    public Message(int statusCode, String message, String httpMethod, String resource, String pathParams, Url url) {
        this.statusCode = statusCode;
        this.message = message;
        this.httpMethod = httpMethod;
        this.resource = resource;
        this.pathParams = pathParams;
        this.url = url;
    }

    public Message(int statusCode, Url url) {
        this.statusCode = statusCode;
        this.url = url;
    }

    public Message(Url url) {
        this.url = url;
    }

    public Message(String httpMethod, String resource, String pathParams, Url url) {
        this.httpMethod = httpMethod;
        this.resource = resource;
        this.pathParams = pathParams;
        this.url = url;
    }

    public Message() {
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Url getUrl() {
        return url;
    }

    public void setUrl(Url url) {
        this.url = url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getPathParams() {
        return pathParams;
    }

    public void setPathParams(String pathParams) {
        this.pathParams = pathParams;
    }
}
