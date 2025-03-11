package com.example.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class FileDownloadHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private DynamoDbClient ddb = DynamoDbClient.create();
    private S3Presigner s3Presigner = S3Presigner.create();

    public FileDownloadHandler() {
        System.out.println("FileDownloadHandler loaded！");
    }

    public FileDownloadHandler(DynamoDbClient dynamoDbClient, S3Presigner s3Presigner) {
        this.ddb = dynamoDbClient;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            // 1. 从请求路径中获取fileId
            String fileId = request.getPathParameters().get("fileId");
            if (fileId == null || fileId.isEmpty()) {
                return createErrorResponse(400, "File ID is required");
            }

            // 2. 从DynamoDB查询文件名
            String fileName = getFileNameFromDynamoDB(fileId);
            if (fileName == null) {
                return createErrorResponse(404, "File not found");
            }

            // 3. 生成预签名URL
            String presignedUrl = generatePresignedUrl(fileName);

            // 4. 返回预签名URL
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(String.format("{\"url\": \"%s\"}", presignedUrl));
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return createErrorResponse(500, "Internal Server Error");
        }
    }

    private String getFileNameFromDynamoDB(String fileId) {
        // 构建查询请求
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("fileId", AttributeValue.builder().s(fileId).build());

        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName("FileMetadata")
                .key(key)
                .build();

        // 执行查询
        GetItemResponse response = ddb.getItem(getItemRequest);

        // 返回文件名
        if (response.hasItem()) {
            return response.item().get("fileName").s();
        } else {
            return null;
        }
    }

    private String generatePresignedUrl(String fileName) {
        // 构建S3 GetObject请求
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket("smart-file-manager-bucket")
                .key(fileName)
                .build();

        // 构建预签名请求
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))  // 预签名URL有效期5分钟
                .getObjectRequest(getObjectRequest)
                .build();

        // 生成预签名URL
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withBody(String.format("{\"error\": \"%s\"}", message));
    }
}