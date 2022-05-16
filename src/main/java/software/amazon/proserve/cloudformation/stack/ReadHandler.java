package software.amazon.proserve.cloudformation.stack;


import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.proserve.cloudformation.stack.translator.Translator;
import software.amazon.proserve.cloudformation.stack.util.ClientBuilder;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<SdkClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();
        final String rolePath = model.getAssumeRolePath() == null ? "" : model.getAssumeRolePath();
        final String roleArn = String.format("arn:aws:iam::%s:role/%s%s", model.getAccountId(), rolePath, model.getAssumeRoleName());
        final String region = model.getRegion() != null ? model.getRegion() : request.getRegion();
        AmazonWebServicesClientProxy _proxy = retrieveCrossAccountProxy(
                proxy,
                (LoggerProxy) logger,
                roleArn,
                region
        );
        ProxyClient<CloudFormationClient> _proxyClient = _proxy.newProxy(() -> ClientBuilder.getCloudFormationClient(region));
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> describeStacks(_proxy, _proxyClient, progress, progress.getResourceModel(), logger, callbackContext))
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }
}
