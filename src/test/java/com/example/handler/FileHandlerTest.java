package com.example.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.MalformedURLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileHandlerTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private DynamoDbClient dynamoDbClient;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private Context context;

    private FileUploadHandler fileUploadHandler;
    private FileDownloadHandler fileDownloadHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fileUploadHandler = new FileUploadHandler(s3Client, dynamoDbClient);
        fileDownloadHandler = new FileDownloadHandler(dynamoDbClient, s3Presigner);
    }

    @Test
    void testFileUploadHandler_Success() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("filename", "test-file.txt");
        request.setQueryStringParameters(queryParams);
        request.setBody(Base64.getEncoder().encodeToString("Hello, World!".getBytes()));

        // Act
        APIGatewayProxyResponseEvent response = fileUploadHandler.handleRequest(request, context);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("fileId"));
    }

    @Test
    void testFileUploadHandler_MissingFilename() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setQueryStringParameters(new HashMap<>());

        // Act
        APIGatewayProxyResponseEvent response = fileUploadHandler.handleRequest(request, context);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Missing filename parameter"));
    }

    @Test
    void testFileUploadHandler_FileSizeExceedsLimit() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("filename", "test-file.txt");
        request.setQueryStringParameters(queryParams);
        request.setBody(Base64.getEncoder().encodeToString(new byte[11 * 1024 * 1024])); // 11MB

        // Act
        APIGatewayProxyResponseEvent response = fileUploadHandler.handleRequest(request, context);

        // Assert
        assertEquals(413, response.getStatusCode());
        assertTrue(response.getBody().contains("File size exceeds 10MB limit"));
    }

    @Test
    void testFileDownloadHandler_Success() throws MalformedURLException {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("fileId", "12345");
        request.setPathParameters(pathParams);

        // Mock DynamoDB response
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("fileName", AttributeValue.builder().s("test-file.txt").build());
        GetItemResponse dynamoResponse = GetItemResponse.builder().item(item).build();
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(dynamoResponse);

        // Mock S3 Presigner response
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new java.net.URL("https://example.com/presigned-url"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        // Act
        APIGatewayProxyResponseEvent response = fileDownloadHandler.handleRequest(request, context);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("url"));
    }

    @Test
    void testFileDownloadHandler_MissingFileId() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPathParameters(new HashMap<>());

        // Act
        APIGatewayProxyResponseEvent response = fileDownloadHandler.handleRequest(request, context);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("File ID is required"));
    }

    @Test
    void testFileDownloadHandler_FileNotFound() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("fileId", "12345");
        request.setPathParameters(pathParams);

        // Mock DynamoDB response (no item found)
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());

        // Act
        APIGatewayProxyResponseEvent response = fileDownloadHandler.handleRequest(request, context);

        // Assert
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("File not found"));
    }
}