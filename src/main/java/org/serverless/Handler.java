package org.serverless;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.dto.Url;
import org.service.UrlService;
import org.util.CustomMessageCodec;
import org.util.Message;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Handler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

	private static final Logger logger = Logger.getLogger(Handler.class);
	private static Vertx vertx;
	static {
		System.setProperty("vertx.disableFileCPResolving", "true");
		vertx = Vertx.vertx();
		final JsonObject jdbcClientConfig = new JsonObject()
				.put("url", System.getenv("db_url"))
				.put("driver_class", System.getenv("db_driver"))
				.put("user", System.getenv("db_username"))
				.put("password", System.getenv("db_password"))
				.put("max_pool_size", 20);
		final JDBCClient client = JDBCClient.createShared(vertx, jdbcClientConfig);
		DeploymentOptions deploymentOptions = new DeploymentOptions().setInstances(Runtime.getRuntime().availableProcessors());
		vertx.deployVerticle(UrlService.class.getName(), deploymentOptions);
		vertx.eventBus().registerDefaultCodec(Message.class, new CustomMessageCodec());
	}

	public Handler() {
		BasicConfigurator.configure();
	}

	@Override
	public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {

		String pathParam = System.getenv("path_param") != null ? System.getenv("path_param") : "shorturl";
		JsonObject jsonRequest = new JsonObject(input);

		logger.debug("RAW INPUT: = " + input);
		logger.debug("INPUT REQUEST = " + jsonRequest.encode());
		final CompletableFuture<Message> future = new CompletableFuture<>();

		Message request = new Message(jsonRequest.getString(HTTP_METHOD), jsonRequest.getString(RESOURCE),
				jsonRequest.getJsonObject("pathParameters") != null ? jsonRequest.getJsonObject("pathParameters").getString(pathParam) : null,
				jsonRequest.getString("body") != null ? new JsonObject(jsonRequest.getString("body")).mapTo(Url.class) : new Url());

		vertx.eventBus().send(input.get(HTTP_METHOD).toString() + input.get(RESOURCE), request, rs -> {
			if(!rs.succeeded()) {
				logger.error("Request id = " + context.getAwsRequestId() + " failed. Cause = " + rs.cause().getMessage());
				future.complete(new Message());
				throw new RuntimeException(rs.cause());
			}
			future.complete((Message) rs.result().body());
		});

		try {
			Message response = future.get(15, TimeUnit.SECONDS);

			switch (response.getStatusCode()) {

				case 200:
					return ApiGatewayResponse.builder()
							.setStatusCode(200)
							.setObjectBody(response.getUrl())
							.build();
				case 307:
					return ApiGatewayResponse
							.builder()
							.setStatusCode(307)
							.setHeaders(Collections.singletonMap("location", response.getUrl().getLongUrl()))
							.build();
				default:
					return ApiGatewayResponse
							.builder()
							.setStatusCode(response.getStatusCode() != 0 ? response.getStatusCode() : 500)
							.setRawBody(response.getMessage() != null ? response.getMessage() : "Internal server error")
							.build();
			}

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.error("Service timeout. Request id = " + context.getAwsRequestId() + " Error = " + e.getMessage());
			return ApiGatewayResponse.builder()
					.setStatusCode(504)
					.setRawBody("Service timeout")
					.build();
		}
	}

	private static final String HTTP_METHOD = "httpMethod";
	private static final String RESOURCE = "resource";
}
