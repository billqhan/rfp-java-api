package com.l3harris.rfp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * AWS client configuration
 */
@Configuration
public class AwsConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(ApiProperties apiProperties) {
        return DynamoDbClient.builder()
                .region(Region.of(apiProperties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public S3Client s3Client(ApiProperties apiProperties) {
        return S3Client.builder()
                .region(Region.of(apiProperties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public LambdaClient lambdaClient(ApiProperties apiProperties) {
        return LambdaClient.builder()
                .region(Region.of(apiProperties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public SqsClient sqsClient(ApiProperties apiProperties) {
        return SqsClient.builder()
                .region(Region.of(apiProperties.getAws().getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}