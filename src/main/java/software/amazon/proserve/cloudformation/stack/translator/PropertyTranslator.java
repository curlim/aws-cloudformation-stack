package software.amazon.proserve.cloudformation.stack.translator;

import com.amazonaws.util.CollectionUtils;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Tag;

import java.util.*;
import java.util.stream.Collectors;

public class PropertyTranslator {
    /**
     * Converts tags (from CFN resource model) to Cfn set (from Cfn SDK)
     *
     * @param tags Tags CFN resource model.
     * @return SDK Tags.
     */
    public static Set<Tag> translateToSdkTags(final Set<software.amazon.proserve.cloudformation.stack.Tag> tags) {
        if (tags == null) {
            return Collections.emptySet();
        }
        return Optional.of(tags).orElse(Collections.emptySet())
                .stream()
                .map(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
                .collect(Collectors.toSet());
    }

    /**
     * Converts a list of tags (from Organization SDK) to Tag set (from CFN resource model)
     *
     * @param tags Tags from SC SDK.
     * @return A set of CFN Tag.
     */
    public static List<software.amazon.proserve.cloudformation.stack.Tag> translateFromSdkTags(final List<Tag> tags) {
        if (CollectionUtils.isNullOrEmpty(tags)) return null;
        return tags.stream().map(tag -> software.amazon.proserve.cloudformation.stack.Tag.builder()
                        .key(tag.key())
                        .value(tag.value())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Converts SDK Parameters to resource model Parameters
     *
     * @param parameters Parameters collection from resource model
     * @return SDK Parameter list
     */
    static List<Parameter> translateToSdkParameters(
            final Collection<software.amazon.proserve.cloudformation.stack.Parameter> parameters) {
        if (software.amazon.awssdk.utils.CollectionUtils.isNullOrEmpty(parameters)) return Collections.emptyList();
        return parameters.stream()
                .map(parameter -> Parameter.builder()
                        .parameterKey(parameter.getKey())
                        .parameterValue(parameter.getValue())
                        .build())
                .collect(Collectors.toList());
    }

}
