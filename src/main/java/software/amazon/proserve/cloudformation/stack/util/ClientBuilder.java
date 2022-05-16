package software.amazon.proserve.cloudformation.stack.util;

import com.amazonaws.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.OrRetryCondition;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {
  public static StsClient getStsClient(String region) {
    final Integer MAX_RETRIES = 10;

    return StsClient.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                    .retryPolicy(RetryPolicy.builder()
                            .backoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                            .throttlingBackoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                            .numRetries(MAX_RETRIES)
                            .retryCondition(OrRetryCondition.create(RetryCondition.defaultRetryCondition(),
                                    LazyHolder.ClientRetryCondition.create()))
                            .build())
                    .build())
            .build();
  }
  public static CloudFormationClient getCloudFormationClient() {
    return LazyHolder.CFN_CLIENT;
  }
  /**
   * Get OrganizationsClient for requests to interact with SC client
   *
   * @return {@link StsClient}
   */
  private static class LazyHolder {
    private static final Integer MAX_RETRIES = 10;

    public static CloudFormationClient CFN_CLIENT = CloudFormationClient.builder()
            .httpClient(LambdaWrapper.HTTP_CLIENT)
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                    .retryPolicy(RetryPolicy.builder()
                            .backoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                            .throttlingBackoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                            .numRetries(MAX_RETRIES)
                            .retryCondition(OrRetryCondition.create(RetryCondition.defaultRetryCondition(),
                                    ClientRetryCondition.create()))
                            .build())
                    .build())
            .build();

    /**
     * Client Throttling Exception StatusCode is 400 while default throttling code is 429
     * https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/main/java/software/amazon/awssdk/core/exception/SdkServiceException.java#L91
     * which means we would need to customize a RetryCondition
     */
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    public static class ClientRetryCondition implements RetryCondition {

      public static ClientRetryCondition create() {
        return new ClientRetryCondition();
      }

      @Override
      public boolean shouldRetry(RetryPolicyContext context) {
        final String errorMessage = context.exception().getMessage();
        if (StringUtils.isNullOrEmpty(errorMessage)) return false;
        if (context.exception() instanceof RetryableException) return true;
        return errorMessage.contains("Rate exceeded");
      }
    }
  }
}
