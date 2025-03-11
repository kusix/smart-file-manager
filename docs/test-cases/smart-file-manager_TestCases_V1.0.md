# Test Case Document for Smart File Manager

## Overview
This document outlines the test cases for a system that allows users to upload and download files. The system involves several components: User, API Gateway, Lambda (FileProcessor), S3 Bucket, and DynamoDB. The test cases are designed based on the sequence diagram provided.

## Test Cases

### Test Case 1: File Upload

**Description:**  
This test case verifies that a user can successfully upload a file to the system.

**Preconditions:**
- The user has a valid file to upload.
- The API Gateway is operational.
- The Lambda function (FileProcessor) is deployed and running.
- The S3 Bucket and DynamoDB are accessible.

**Steps:**
1. User sends a PUT request to the API Gateway at the `/upload` endpoint with the file to be uploaded.
2. API Gateway invokes the Lambda function (FileProcessor).
3. Lambda function stores the original file in the S3 Bucket.
4. Lambda function stores the file's meta-data in DynamoDB.
5. Lambda function returns a file ID to the user.

**Expected Results:**
- The file is successfully stored in the S3 Bucket.
- The file's meta-data is correctly stored in DynamoDB.
- The user receives a unique file ID.

### Test Case 2: File Download

**Description:**  
This test case verifies that a user can successfully download a previously uploaded file using the file ID.

**Preconditions:**
- The file has been previously uploaded and a file ID has been issued.
- The API Gateway is operational.
- The Lambda function (FileProcessor) is deployed and running.
- The S3 Bucket and DynamoDB are accessible.

**Steps:**
1. User sends a GET request to the API Gateway at the `/download/{fileId}` endpoint with the file ID.
2. API Gateway invokes the Lambda function (FileProcessor).
3. Lambda function fetches the file's meta-data from DynamoDB.
4. Lambda function generates a pre-signed URL for the file stored in the S3 Bucket.
5. Lambda function returns the pre-signed URL to the user.

**Expected Results:**
- The Lambda function successfully retrieves the file's meta-data from DynamoDB.
- A valid pre-signed URL is generated for the file in the S3 Bucket.
- The user receives the pre-signed URL and can download the file.

## Conclusion
These test cases cover the primary functionalities of the file upload and download system. They ensure that the system components interact correctly and that the user can successfully upload and download files as expected.