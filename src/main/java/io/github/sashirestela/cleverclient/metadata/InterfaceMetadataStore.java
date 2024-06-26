package io.github.sashirestela.cleverclient.metadata;

import io.github.sashirestela.cleverclient.annotation.HttpMethod;
import io.github.sashirestela.cleverclient.metadata.InterfaceMetadata.AnnotationMetadata;
import io.github.sashirestela.cleverclient.metadata.InterfaceMetadata.MethodMetadata;
import io.github.sashirestela.cleverclient.metadata.InterfaceMetadata.ParameterMetadata;
import io.github.sashirestela.cleverclient.support.CleverClientException;
import io.github.sashirestela.cleverclient.support.ReturnType;
import io.github.sashirestela.cleverclient.util.CommonUtil;
import io.github.sashirestela.cleverclient.util.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterfaceMetadataStore {

    private static Logger logger = LoggerFactory.getLogger(InterfaceMetadataStore.class);

    private static InterfaceMetadataStore store = null;

    private Map<String, InterfaceMetadata> interfacesByFullName;

    private InterfaceMetadataStore() {
        interfacesByFullName = new HashMap<>();
    }

    public static InterfaceMetadataStore one() {
        if (store == null) {
            store = new InterfaceMetadataStore();
        }
        return store;
    }

    public void save(Class<?> interfaceClass) {
        if (interfacesByFullName.containsKey(interfaceClass.getName())) {
            return;
        }
        Map<String, MethodMetadata> methodBySignature = new HashMap<>();
        for (var javaMethod : interfaceClass.getMethods()) {
            var methodMetadata = MethodMetadata.builder()
                    .name(javaMethod.getName())
                    .returnType(new ReturnType(javaMethod))
                    .isDefault(javaMethod.isDefault())
                    .annotations(getAnnotations(javaMethod.getDeclaredAnnotations()))
                    .parameters(getParameters(javaMethod.getParameters()))
                    .build();
            methodBySignature.put(javaMethod.toString(), methodMetadata);
        }
        var interfaceMetadata = InterfaceMetadata.builder()
                .name(interfaceClass.getSimpleName())
                .annotations(getAnnotations(interfaceClass.getDeclaredAnnotations()))
                .methodBySignature(methodBySignature)
                .build();

        validate(interfaceMetadata);

        interfacesByFullName.put(interfaceClass.getName(), interfaceMetadata);
        logger.debug("The interface {} was saved", interfaceClass.getSimpleName());
    }

    public InterfaceMetadata get(Class<?> interfaceClass) {
        if (interfacesByFullName.containsKey(interfaceClass.getName())) {
            return interfacesByFullName.get(interfaceClass.getName());
        } else {
            throw new CleverClientException("The interface {0} has not been saved yet.", interfaceClass.getSimpleName(),
                    null);
        }
    }

    private List<AnnotationMetadata> getAnnotations(Annotation[] javaAnnotations) {
        List<AnnotationMetadata> annotations = new ArrayList<>();
        boolean isAnnotArray = false;
        for (var javaAnnotation : javaAnnotations) {
            Map<String, String> valueByField = new HashMap<>();
            for (var javaAnnotMethod : javaAnnotation.annotationType().getDeclaredMethods()) {
                Object object = getAnnotationValue(javaAnnotation, javaAnnotMethod);
                if (object instanceof Annotation[]) {
                    isAnnotArray = true;
                    annotations.addAll(getAnnotations((Annotation[]) object));
                } else {
                    isAnnotArray = false;
                    var field = javaAnnotMethod.getName();
                    var value = object instanceof String ? (String) object : null;
                    valueByField.put(field, value);
                }
            }
            if (!isAnnotArray) {
                var annotationMetadata = AnnotationMetadata.builder()
                        .name(javaAnnotation.annotationType().getSimpleName())
                        .isHttpMethod(javaAnnotation.annotationType().isAnnotationPresent(HttpMethod.class))
                        .valueByField(valueByField)
                        .build();
                annotations.add(annotationMetadata);
            }
        }
        return annotations;
    }

    private List<ParameterMetadata> getParameters(Parameter[] javaParameters) {
        List<ParameterMetadata> parameters = new ArrayList<>();
        var index = 0;
        for (var javaParameter : javaParameters) {
            var annotations = getAnnotations(javaParameter.getDeclaredAnnotations());
            var parameterMetadata = ParameterMetadata.builder()
                    .index(index++)
                    .annotation(!annotations.isEmpty() ? annotations.get(0) : null)
                    .build();
            parameters.add(parameterMetadata);
        }
        return parameters;
    }

    private void validate(InterfaceMetadata interfaceMetadata) {
        interfaceMetadata.getMethodBySignature().forEach((methodSignature, methodMetadata) -> {
            if (!methodMetadata.isDefault()) {
                if (!methodMetadata.hasHttpAnnotation()) {
                    throw new CleverClientException("Missing HTTP annotation for the method {0}.",
                            methodMetadata.getName(), null);
                }
                var url = interfaceMetadata.getFullUrlByMethod(methodMetadata);
                var listPathParams = CommonUtil.findFullMatches(url, Constant.REGEX_PATH_PARAM_URL);
                if (!CommonUtil.isNullOrEmpty(listPathParams)) {
                    listPathParams.forEach(pathParam -> methodMetadata.getPathParameters()
                            .stream()
                            .map(parameter -> parameter.getAnnotation().getValue())
                            .filter(paramAnnotValue -> pathParam.equals(paramAnnotValue))
                            .findFirst()
                            .orElseThrow(() -> new CleverClientException(
                                    "Path param {0} in the url cannot find an annotated argument in the method {1}.",
                                    pathParam, methodMetadata.getName(), null)));
                }
            }
        });
    }

    private Object getAnnotationValue(Annotation javaAnnotation, Method javaAnnotMethod) {
        Object object;
        try {
            object = javaAnnotMethod.invoke(javaAnnotation, (Object[]) null);
        } catch (Exception e) {
            object = null;
        }
        return object;
    }

}
