package io.lolyay.discordmsend.server.upload;

import io.lolyay.discordmsend.server.config.ConfigFile;
import io.lolyay.discordmsend.util.logging.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.net.URI;

/**
 * Handles uploading files to a S3 bucket.
 */
public class S3UploadManager {
    
    private final S3Client s3Client;
    private final String bucketName;
    private final String baseUrl;
    
    public S3UploadManager(String accessKey, String secretKey, String region, String bucketUrl, String publicUrl) {
        this.bucketName = extractBucketName(bucketUrl);
        this.baseUrl = publicUrl;
        
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        // Configure S3 for path-style access (required for some S3-compatible services)
        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
        
        Region awsRegion = Region.of(region);
        String endpoint = extractEndpoint(bucketUrl);
        
        this.s3Client = S3Client.builder()
                .region(awsRegion)
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(s3Config)
                .build();
        
        Logger.info("S3 Upload Manager initialized");
        Logger.info("  Bucket: " + bucketName);
        Logger.info("  Region: " + region);
        Logger.info("  Endpoint: " + endpoint);
        Logger.info("  Public URL: " + publicUrl);
    }
    
    /**
     * Uploads a file to S3 and returns the public URL.
     * 
     * @param file The file to upload
     * @param cacheId The cache ID (used as filename)
     * @return The public URL of the uploaded file
     * @throws Exception if upload fails
     */
    public String uploadTrack(File file, String cacheId) throws Exception {
        String key = "pub/tracks/" + cacheId + ".mp3";
        
        try {
            Logger.debug("Uploading to S3: bucket=" + bucketName + ", key=" + key + ", size=" + file.length() + " bytes");
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("audio/mpeg")
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
            
            String url = baseUrl + "/" + key;
            Logger.info("Successfully uploaded track to S3: " + url);
            return url;
            
        } catch (S3Exception e) {
            Logger.err("S3 upload failed: " + e.awsErrorDetails().errorMessage());
            throw new Exception("S3 upload failed: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            Logger.err("Upload error: " + e.getClass().getName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                Logger.err("Caused by: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            }
            throw new Exception("Upload failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts bucket name
     */
    private String extractBucketName(String bucketUrl) {
        String[] parts = bucketUrl.split("/");
        return parts[parts.length - 1];
    }
    
    /**
     * Extracts endpoint from bucket URL (everything before the bucket name)
     */
    private String extractEndpoint(String bucketUrl) {
        int lastSlash = bucketUrl.lastIndexOf("/");
        return bucketUrl.substring(0, lastSlash);
    }
    
    /**
     * Closes the S3 client. (isn't this obvious)
     */
    public void close() {
        s3Client.close();
    }
}
