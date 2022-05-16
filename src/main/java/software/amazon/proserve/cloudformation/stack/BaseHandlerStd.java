package software.amazon.proserve.cloudformation.stack;

import com.google.common.annotations.VisibleForTesting;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.cloudformation.exceptions.TerminalException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.cloudformation.proxy.delay.MultipleOf;
import software.amazon.proserve.cloudformation.stack.translator.Translator;
import software.amazon.proserve.cloudformation.stack.util.ClientBuilder;

import java.time.Duration;
import java.util.*;

import static software.amazon.awssdk.services.cloudformation.model.StackStatus.CREATE_COMPLETE;
import static software.amazon.proserve.cloudformation.stack.translator.Translator.*;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
  protected static final MultipleOf MULTIPLE_OF = MultipleOf.multipleOf()
          .multiple(2)
          .timeout(Duration.ofHours(24L))
          .delay(Duration.ofSeconds(2L))
          .build();

  @Override
  public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
    final AmazonWebServicesClientProxy proxy,
    final ResourceHandlerRequest<ResourceModel> request,
    final CallbackContext callbackContext,
    final Logger logger) {
    logger.log(request.getDesiredResourceState().toString());
    final ResourceModel model = request.getDesiredResourceState();

    return handleRequest(
      proxy,
      request,
      callbackContext != null ? callbackContext : new CallbackContext(),
      proxy.newProxy(() -> ClientBuilder.getStsClient(model.getRegion() != null ? model.getRegion() : request.getRegion())),
      logger
    );
  }

  protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
    final AmazonWebServicesClientProxy proxy,
    final ResourceHandlerRequest<ResourceModel> request,
    final CallbackContext callbackContext,
    final ProxyClient<SdkClient> proxyClient,
    final Logger logger);

  protected static AmazonWebServicesClientProxy retrieveCrossAccountProxy(AmazonWebServicesClientProxy proxy, LoggerProxy loggerProxy, String roleArn, String region) {
    ProxyClient<StsClient> proxyClient = proxy.newProxy(() -> ClientBuilder.getStsClient(region));
    AssumeRoleResponse assumeRoleResponse = proxyClient.injectCredentialsAndInvokeV2(
            createAssumeRoleRequest(roleArn),
            proxyClient.client()::assumeRole
    );

    software.amazon.awssdk.services.sts.model.Credentials credentials = assumeRoleResponse.credentials();
    Credentials cfnCredentials = new Credentials(credentials.accessKeyId(), credentials.secretAccessKey(), credentials.sessionToken());
    return new AmazonWebServicesClientProxy(
            loggerProxy,
            cfnCredentials,
            DelayFactory.CONSTANT_DEFAULT_DELAY_FACTORY,
            WaitStrategy.scheduleForCallbackStrategy()
    );
  }

  protected ProgressEvent<ResourceModel, CallbackContext> createStack(
          final AmazonWebServicesClientProxy proxy,
          final ProxyClient<CloudFormationClient> proxyClient,
          final ProgressEvent<ResourceModel, CallbackContext> progress,
          final ResourceModel model,
          final Logger logger,
          final CallbackContext ctx
  ) {
    return proxy
            .initiate("ProServe-CloudFormation-Stack::CreateStack", proxyClient, model, progress.getCallbackContext())
            .translateToServiceRequest(Translator::translateToCreateStack)
            .backoffDelay(MULTIPLE_OF)
            .makeServiceCall((modelRequest, proxyInvocation) -> {
              final CreateStackResponse response = proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::createStack);
              logger.log(String.format("[%s] stack creation initiated", response.stackId()));
              return response;
            })
            .stabilize((_request, response, proxyInvocation, resourceModel, context) -> {
              resourceModel.setStackId(response.stackId());
              //TODO: set outputs and tags
              resourceModel.setOutput("");
              return isOperationStabilized(proxyInvocation, resourceModel, logger);
            })
            .progress();
  }

  protected ProgressEvent<ResourceModel, CallbackContext> updateStack(
          final AmazonWebServicesClientProxy proxy,
          final ProxyClient<CloudFormationClient> proxyClient,
          final ProgressEvent<ResourceModel, CallbackContext> progress,
          final ResourceModel model,
          final Logger logger,
          final CallbackContext ctx
  ) {
    return proxy
            .initiate("ProServe-CloudFormation-Stack::UpdateStack", proxyClient, model, progress.getCallbackContext())
            .translateToServiceRequest(Translator::translateToUpdateStack)
            .backoffDelay(MULTIPLE_OF)
            .makeServiceCall((modelRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::updateStack))
            .stabilize((_request, response, proxyInvocation, resourceModel, context) -> {
              resourceModel.setStackId(response.stackId());
              //TODO: set outputs and tags
              resourceModel.setOutput("");
              return isOperationStabilized(proxyInvocation, resourceModel, logger);
            })
            .handleError((_request, e, _proxyClient, _model, context) -> {
              if (e instanceof CloudFormationException && e.getMessage().contains("No updates are to be performed")) {
                return ProgressEvent.progress(_model, context);
              } else {
                throw e;
              }
            })
            .progress();
  }

  protected ProgressEvent<ResourceModel, CallbackContext> deleteStack(
          final AmazonWebServicesClientProxy proxy,
          final ProxyClient<CloudFormationClient> proxyClient,
          final ProgressEvent<ResourceModel, CallbackContext> progress,
          final ResourceModel model,
          final Logger logger,
          final CallbackContext ctx
  ) {
    return proxy
            .initiate("ProServe-CloudFormation-Stack::DeleteStack", proxyClient, model, progress.getCallbackContext())
            .translateToServiceRequest(Translator::translateToDeleteStack)
            .backoffDelay(MULTIPLE_OF)
            .makeServiceCall((modelRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::deleteStack))
            .stabilize((_request, response, proxyInvocation, resourceModel, context) -> isOperationStabilized(proxyInvocation, resourceModel, logger))
            .progress();
  }

  protected ProgressEvent<ResourceModel, CallbackContext> describeStacks(
          final AmazonWebServicesClientProxy proxy,
          final ProxyClient<CloudFormationClient> proxyClient,
          final ProgressEvent<ResourceModel, CallbackContext> progress,
          final ResourceModel model,
          final Logger logger,
          final CallbackContext ctx
  ) {
    return proxy
            .initiate("ProServe-CloudFormation-Stack::DescribeStack", proxyClient, model, progress.getCallbackContext())
            .translateToServiceRequest(Translator::translateToDescribeStack)
            .backoffDelay(MULTIPLE_OF)
            .makeServiceCall((modelRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::describeStacks))
            .stabilize((_request, response, proxyInvocation, resourceModel, context) -> {
//              resourceModel.setStackId(response.stackId());
              //TODO: set outputs and tags
              resourceModel.setOutput("");
              return true;
            })
            .progress();
  }

  protected Stack describeStack(
          final ProxyClient<CloudFormationClient> proxyClient,
          final ResourceModel model,
          final Logger logger
  ) {
    final DescribeStacksResponse response = proxyClient.injectCredentialsAndInvokeV2(
            translateToDescribeStack(model),
            proxyClient.client()::describeStacks);
    return response.stacks().get(0);
  }

  /**
   * Checks if the operation is stabilized using OperationId to interact with
   * {@link DescribeStacksResponse}
   *
   * @param model       {@link ResourceModel}
   * @param logger      Logger
   * @return A boolean value indicates if operation is complete
   */
  protected boolean isOperationStabilized(final ProxyClient<CloudFormationClient> proxyClient,
                                          final ResourceModel model,
                                          final Logger logger) {

    final Stack stack = describeStack(proxyClient, model, logger);
    final boolean isSucceeded = isStackSucceeded(stack, logger);

//    if (isSucceeded && recordDetail.recordType().compareTo("TERMINATE_PROVISIONED_PRODUCT") != 0) {
//      Map<String, String> outputs = translateFromSdkOutputs(getProvisionedProductOutputs(proxyClient, model));
//
//      model.setOutputs(outputs);
//      List<RecordTag> tags = recordDetail.recordTags() == null ? new LinkedList<>() : recordDetail.recordTags();
//      Optional<RecordTag> cfnOutputKeyTag = tags.stream().filter(tag -> tag.key().compareTo("proserve:CfnOutputKey") == 0).findFirst();
//      String outputValue = cfnOutputKeyTag.isPresent() ? Objects.requireNonNull(outputs).get(cfnOutputKeyTag.get().value()) : recordId;
//      model.setOutputValue(outputValue);
//    }

    return isSucceeded;
  }

  /**
   * Compares {@link software.amazon.awssdk.services.cloudformation.model.StackStatus} with specific statuses
   *
   * @param stack      {@link Stack}
   * @return boolean
   */
  @VisibleForTesting
  protected static boolean isStackSucceeded(
          final Stack stack, final Logger logger) {

    switch (stack.stackStatus()) {
      case UPDATE_COMPLETE:
      case DELETE_COMPLETE:
      case CREATE_COMPLETE:
        logger.log(String.format("Stack [%s] has been successfully stabilized.", stack.stackId()));
        return true;
      case CREATE_IN_PROGRESS:
      case UPDATE_IN_PROGRESS:
      case UPDATE_COMPLETE_CLEANUP_IN_PROGRESS:
      case UPDATE_ROLLBACK_IN_PROGRESS:
      case UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS:
      case DELETE_IN_PROGRESS:
      case ROLLBACK_IN_PROGRESS:
        return false;
      default:
        logger.log(String.format("Stack [%s] unexpected status [%s]", stack.stackId(), stack.stackStatus()));
        throw new TerminalException(
                String.format("Stack [%s] was unexpectedly stopped or failed, reason: [%s]", stack.stackId(), stack.stackStatusReason()));
    }
  }
}
