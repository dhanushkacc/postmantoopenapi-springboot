package com.example.postmanopenapi.service;

import com.example.postmanopenapi.config.ApiProperties;
import com.example.postmanopenapi.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class ApiServiceTest {

    @Mock
    private ApiProperties apiProperties;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ApiService apiService; // Will be spied in setUp

    private ListAppender<ILoggingEvent> listAppender;
    private Logger apiServiceLogger;

    private final String DUMMY_COLLECTION_UID = "dummy-uid";
    private final String DUMMY_BASE_URL = "http://localhost:8080";
    private final String EXPECTED_EXCEPTION_MESSAGE = "OpenAPI specification 'output' field is missing or not text in the response from generation service.";

    @BeforeEach
    void setUp() {
        apiServiceLogger = (Logger) LoggerFactory.getLogger(ApiService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        apiServiceLogger.addAppender(listAppender);

        // Manually create a spy of the apiService instance after @InjectMocks has done its work.
        apiService = Mockito.spy(apiService);

        // Make this stubbing lenient as it's not used by all tests,
        // specifically the generate_shouldLogErrorMessage_whenDeleteCollectionFails test.
        Mockito.lenient().when(apiProperties.getBaseUrl()).thenReturn(DUMMY_BASE_URL);
    }

    // Add an @AfterEach to clean up the appender
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (listAppender != null) {
            listAppender.stop();
            listAppender.list.clear();
        }
        if (apiServiceLogger != null) {
            apiServiceLogger.detachAppender(listAppender);
        }
    }

    @Test
    void generateOpenApiSpec_shouldThrowApiException_whenOutputFieldIsMissing() throws Exception {
        // Arrange
        String responseBody = "{ \"some_other_field\": \"value\" }";
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        JsonNode mockNode = new ObjectMapper().readTree(responseBody); // Use real ObjectMapper to create node

        Mockito.when(restTemplate.exchange(
                eq(DUMMY_BASE_URL + "/collections/" + DUMMY_COLLECTION_UID + "/transformations"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockResponseEntity);

        Mockito.when(objectMapper.readTree(responseBody)).thenReturn(mockNode);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            apiService.generateOpenApiSpec(DUMMY_COLLECTION_UID);
        });

        assertEquals(EXPECTED_EXCEPTION_MESSAGE, exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
    }

    @Test
    void generateOpenApiSpec_shouldThrowApiException_whenOutputFieldIsNotTextual() throws Exception {
        // Arrange
        String responseBody = "{ \"output\": 123 }";
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        JsonNode mockNode = new ObjectMapper().readTree(responseBody);

        Mockito.when(restTemplate.exchange(
                eq(DUMMY_BASE_URL + "/collections/" + DUMMY_COLLECTION_UID + "/transformations"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockResponseEntity);

        Mockito.when(objectMapper.readTree(responseBody)).thenReturn(mockNode);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            apiService.generateOpenApiSpec(DUMMY_COLLECTION_UID);
        });

        assertEquals(EXPECTED_EXCEPTION_MESSAGE, exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
    }

    @Test
    void generateOpenApiSpec_shouldReturnSpec_whenOutputIsValid() throws Exception {
        // Arrange
        String expectedSpec = "valid_openapi_spec_content";
        String responseBody = "{ \"output\": \"" + expectedSpec + "\" }";
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        JsonNode mockNode = new ObjectMapper().readTree(responseBody);

        Mockito.when(restTemplate.exchange(
                eq(DUMMY_BASE_URL + "/collections/" + DUMMY_COLLECTION_UID + "/transformations"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockResponseEntity);

        Mockito.when(objectMapper.readTree(responseBody)).thenReturn(mockNode);

        // Act
        String actualSpec = apiService.generateOpenApiSpec(DUMMY_COLLECTION_UID);

        // Assert
        assertEquals(expectedSpec, actualSpec);
    }

    @Test
    void generateOpenApiSpec_shouldThrowApiException_whenRestTemplateThrowsException() {
        // Arrange
        Mockito.when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("Connection refused"));

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            apiService.generateOpenApiSpec(DUMMY_COLLECTION_UID);
        });

        assertEquals("Failed to generate OpenAPI spec: Connection refused", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void generateOpenApiSpec_shouldThrowApiException_whenExchangeReturnsNotOk() {
        // Arrange
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>("Error", HttpStatus.NOT_FOUND);

        Mockito.when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockResponseEntity);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            apiService.generateOpenApiSpec(DUMMY_COLLECTION_UID);
        });

        assertEquals("Request failed with status: " + HttpStatus.NOT_FOUND, exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

     @Test
    void generateOpenApiSpec_shouldThrowApiException_whenObjectMapperThrowsException() throws Exception {
        // Arrange
        String responseBody = "{ \"output\": \"valid_spec\" }"; // Valid structure
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        Mockito.when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockResponseEntity);

        // Mock ObjectMapper to throw an exception when readTree is called
        Mockito.when(objectMapper.readTree(responseBody))
               .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Parsing error") {});

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            apiService.generateOpenApiSpec(DUMMY_COLLECTION_UID);
        });

        assertTrue(exception.getMessage().startsWith("Error parsing response: Parsing error"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
    }

    @Test
    void generate_shouldLogErrorMessage_whenDeleteCollectionFails() throws Exception {
        // Arrange
        String dummyCollectionJson = "{\"info\": {\"name\": \"Test Collection\"}}";
        String collectionUid = "test-uid";
        String dummySpec = "dummy-openapi-spec";
        String expectedLogMessage = "Something failed: Deletion failed";
        String deletionExceptionMessage = "Deletion failed";

        // Mock internal calls using doReturn for spied object
        Mockito.doReturn(collectionUid).when(apiService).createCollection(anyString());
        // generateOpenApiSpec is public and part of the spied object, so it's also "mocked" or spied upon
        Mockito.doReturn(dummySpec).when(apiService).generateOpenApiSpec(eq(collectionUid));
        Mockito.doThrow(new RuntimeException(deletionExceptionMessage)).when(apiService).deleteCollection(eq(collectionUid));

        // Act
        com.example.postmanopenapi.dto.ResponseDTO response = apiService.generate(dummyCollectionJson);

        // Assert
        assertNotNull(response);
        assertEquals(dummySpec, response.getOpenApiSpec());

        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.stream()
                        .anyMatch(event -> event.getLevel() == Level.ERROR &&
                                event.getFormattedMessage().contains(expectedLogMessage) &&
                                event.getThrowableProxy() != null &&
                                event.getThrowableProxy().getClassName().equals(RuntimeException.class.getName()) &&
                                event.getThrowableProxy().getMessage().equals(deletionExceptionMessage)),
                "Expected log message not found or throwable did not match.");
    }
}
