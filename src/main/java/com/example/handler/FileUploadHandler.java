package com.example.handler;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FileUploadHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final S3Client s3 = S3Client.create();
    private final DynamoDbClient ddb = DynamoDbClient.create();

    public FileUploadHandler() {
        System.out.println("FileUploadHandler loaded！");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            // 参数校验
            if (request.getQueryStringParameters() == null ||
                    !request.getQueryStringParameters().containsKey("filename")) {
                return createErrorResponse(400, "Missing filename parameter");
            }

            String fileName = request.getQueryStringParameters().get("filename");
            byte[] fileContent = Base64.getDecoder().decode(request.getBody());

            if (fileContent.length > 10 * 1024 * 1024) { // 10MB限制
                return createErrorResponse(413, "File size exceeds 10MB limit");
            }

            // 存储到S3
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket("smart-file-manager-bucket")
                    .key(fileName)
                    .build();
            s3.putObject(putRequest, RequestBody.fromBytes(fileContent));

            // 生成唯一ID并存储元数据
            String fileId = UUID.randomUUID().toString();
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("fileId", AttributeValue.builder().s(fileId).build());
            item.put("fileName", AttributeValue.builder().s(fileName).build());
            item.put("uploadTime", AttributeValue.builder().s(java.time.Instant.now().toString()).build());

            ddb.putItem(PutItemRequest.builder()
                    .tableName("FileMetadata")
                    .item(item)
                    .build());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(String.format("{\"fileId\": \"%s\"}", fileId));
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return createErrorResponse(500, "Internal Server Error");
        }
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int code, String message) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(code)
                .withBody(String.format("{\"error\": \"%s\"}", message));
    }
}