package software.amazon.proserve.cloudformation.stack;

import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.proxy.*;
import software.amazon.proserve.cloudformation.stack.util.ClientBuilder;


public class CreateHandler extends BaseHandlerStd {
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
        AmazonWebServicesClientProxy _proxy = retrieveCrossAccountProxy(
                proxy,
                (LoggerProxy) logger,
                roleArn,
                model.getRegion() != null ? model.getRegion() : request.getRegion()
        );
        ProxyClient<CloudFormationClient> _proxyClient = _proxy.newProxy(ClientBuilder::getCloudFormationClient);
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> createStack(_proxy, _proxyClient, progress, progress.getResourceModel(), logger, callbackContext))
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }
}
