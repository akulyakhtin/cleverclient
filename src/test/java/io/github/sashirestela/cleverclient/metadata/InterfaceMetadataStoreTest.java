package io.github.sashirestela.cleverclient.metadata;

import io.github.sashirestela.cleverclient.http.ITest;
import io.github.sashirestela.cleverclient.metadata.InterfaceMetadata.AnnotationMetadata;
import io.github.sashirestela.cleverclient.metadata.InterfaceMetadata.MethodMetadata;
import io.github.sashirestela.cleverclient.metadata.InterfaceMetadata.ParameterMetadata;
import io.github.sashirestela.cleverclient.support.CleverClientException;
import io.github.sashirestela.cleverclient.support.ReturnType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;

import static io.github.sashirestela.cleverclient.util.CommonUtil.createMapString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterfaceMetadataStoreTest {

    InterfaceMetadataStore store = InterfaceMetadataStore.one();

    @Test
    void shouldSaveInterfaceMetadataWhenAccomplishValidtion() {
        var methodBySignature = new HashMap<String, MethodMetadata>();
        methodBySignature.put(
                "public abstract io.github.sashirestela.cleverclient.http.ITest$Demo io.github.sashirestela.cleverclient.http.ITest$GoodService.demoPostMethod(io.github.sashirestela.cleverclient.http.ITest$RequestDemo,java.lang.Long)",
                MethodMetadata.builder()
                        .name("demoPostMethod")
                        .returnType(new ReturnType("io.github.sashirestela.cleverclient.http.ITest$Demo"))
                        .isDefault(false)
                        .annotations(Arrays.asList(
                                AnnotationMetadata.builder()
                                        .name("POST")
                                        .isHttpMethod(true)
                                        .valueByField(createMapString("value", "/demos/{demoId}"))
                                        .build(),
                                AnnotationMetadata.builder()
                                        .name("Multipart")
                                        .isHttpMethod(false)
                                        .valueByField(createMapString())
                                        .build(),
                                AnnotationMetadata.builder()
                                        .name("Header")
                                        .isHttpMethod(false)
                                        .valueByField(createMapString("name", "ThirdKey", "value", "ThirdVal"))
                                        .build()))
                        .parameters(Arrays.asList(
                                ParameterMetadata.builder()
                                        .index(0)
                                        .annotation(AnnotationMetadata.builder()
                                                .name("Body")
                                                .isHttpMethod(false)
                                                .valueByField(createMapString())
                                                .build())
                                        .build(),
                                ParameterMetadata.builder()
                                        .index(1)
                                        .annotation(AnnotationMetadata.builder()
                                                .name("Path")
                                                .isHttpMethod(false)
                                                .valueByField(createMapString("value", "demoId"))
                                                .build())
                                        .build()))
                        .build());
        methodBySignature.put(
                "public abstract java.util.List io.github.sashirestela.cleverclient.http.ITest$GoodService.demoGetMethod(java.lang.Long,java.lang.Integer,java.lang.Integer)",
                MethodMetadata.builder()
                        .name("demoGetMethod")
                        .returnType(new ReturnType(
                                "java.util.List<io.github.sashirestela.cleverclient.http.ITest$Demo>"))
                        .isDefault(false)
                        .annotations(Arrays.asList(
                                AnnotationMetadata.builder()
                                        .name("GET")
                                        .isHttpMethod(true)
                                        .valueByField(createMapString("value", "/demos/{demoId}/subdemos"))
                                        .build()))
                        .parameters(Arrays.asList(
                                ParameterMetadata.builder()
                                        .index(0)
                                        .annotation(AnnotationMetadata.builder()
                                                .name("Path")
                                                .isHttpMethod(false)
                                                .valueByField(createMapString("value", "demoId"))
                                                .build())
                                        .build(),
                                ParameterMetadata.builder()
                                        .index(1)
                                        .annotation(AnnotationMetadata.builder()
                                                .name("Query")
                                                .isHttpMethod(false)
                                                .valueByField(createMapString("value", "size"))
                                                .build())
                                        .build(),
                                ParameterMetadata.builder()
                                        .index(2)
                                        .annotation(AnnotationMetadata.builder()
                                                .name("Query")
                                                .isHttpMethod(false)
                                                .valueByField(createMapString("value", "page"))
                                                .build())
                                        .build()))
                        .build());
        var expectedInterfaceMetadata = InterfaceMetadata.builder()
                .name("GoodService")
                .annotations(Arrays.asList(
                        AnnotationMetadata.builder()
                                .name("Resource")
                                .isHttpMethod(false)
                                .valueByField(createMapString("value", "/api"))
                                .build(),
                        AnnotationMetadata.builder()
                                .name("Header")
                                .isHttpMethod(false)
                                .valueByField(createMapString("name", "FirstKey", "value", "FirstVal"))
                                .build(),
                        AnnotationMetadata.builder()
                                .name("Header")
                                .isHttpMethod(false)
                                .valueByField(createMapString("name", "SecondKey", "value", "SecondVal"))
                                .build()))
                .methodBySignature(methodBySignature)
                .build();
        var interfaceClass = ITest.GoodService.class;
        store.save(interfaceClass);
        var actualInterfaceMetadata = store.get(interfaceClass);
        assertEquals(expectedInterfaceMetadata.toString(), actualInterfaceMetadata.toString());
    }

    @Test
    void shouldThrownExceptionWhenTryingToGetNotPreviouslySavedInterface() {
        Exception exception = assertThrows(CleverClientException.class,
                () -> store.get(ITest.NotSavedService.class));
        assertEquals("The interface NotSavedService has not been saved yet.", exception.getMessage());
    }

    @Test
    void shouldThrownExceptionWhenMethodHasNotHttpAnnotation() {
        Exception exception = assertThrows(CleverClientException.class,
                () -> store.save(ITest.NotAnnotatedService.class));
        assertEquals("Missing HTTP annotation for the method unannotatedMethod.",
                exception.getMessage());
    }

    @Test
    void shouldThrownExceptionWhenUrlPathParamAtMethodUnmatchesAnnotatedArguments() {
        Exception exception = assertThrows(CleverClientException.class,
                () -> store.save(ITest.BadPathParamService.class));
        assertEquals(
                "Path param demoId in the url cannot find an annotated argument in the method unmatchedPathParamMethod.",
                exception.getMessage());
    }

    @Test
    void shouldSaveSuccesfullyWhenInterfaceHasResourcePathParamAndDefaultMethod() {
        assertDoesNotThrow(() -> store.save(ITest.WithResourcePathParamAndDefaultMethods.class));
    }

}
