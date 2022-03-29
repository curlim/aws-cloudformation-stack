package software.amazon.proserve.cloudformation.stack.translator;

import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.proserve.cloudformation.stack.ResourceModel;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static software.amazon.proserve.cloudformation.stack.translator.PropertyTranslator.translateToSdkParameters;
import static software.amazon.proserve.cloudformation.stack.translator.PropertyTranslator.translateToSdkTags;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  public static AssumeRoleRequest createAssumeRoleRequest(final String roleArn) {
    return AssumeRoleRequest.builder()
            .roleArn(roleArn)
            .roleSessionName("proserve-cloudformation-stack")
            .build();
  }

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  public static CreateStackRequest translateToCreateStack(final ResourceModel model) {
    return CreateStackRequest
            .builder()
            .stackName(model.getStackName())
            .parameters(translateToSdkParameters(model.getParameters()))
            .tags(translateToSdkTags(model.getTags()))
            .templateBody(model.getTemplate())
            .templateURL(model.getTemplateUrl())
            .capabilitiesWithStrings(model.getCapabilities())
            .build();
  }

  /**
   * Request to update a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  public static UpdateStackRequest translateToUpdateStack(final ResourceModel model) {
    return UpdateStackRequest
            .builder()
            .stackName(model.getStackName())
            .parameters(translateToSdkParameters(model.getParameters()))
            .tags(translateToSdkTags(model.getTags()))
            .templateBody(model.getTemplate())
            .templateURL(model.getTemplateUrl())
            .capabilitiesWithStrings(model.getCapabilities())
            .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  public static DeleteStackRequest translateToDeleteStack(final ResourceModel model) {
    return DeleteStackRequest
            .builder()
            .stackName(model.getStackId())
            .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  public static DescribeStacksRequest translateToDescribeStack(final ResourceModel model) {
    return DescribeStacksRequest
            .builder()
            .stackName(model.getStackId())
            .build();
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }
}
