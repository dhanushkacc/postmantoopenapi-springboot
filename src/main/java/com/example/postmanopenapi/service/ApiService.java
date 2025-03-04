package com.example.postmanopenapi.service;

import com.example.postmanopenapi.config.ApiProperties;
import com.example.postmanopenapi.dto.ResponseDTO;
import com.example.postmanopenapi.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpStatus;

@Service
@Slf4j
public class ApiService {

    @Autowired
    private ApiProperties apiProperties;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public ResponseDTO generate(String collectionJson) {
        String collectionUid = null;
        try {
            collectionUid = createCollection(collectionJson);
            String openApiSpec = generateOpenApiSpec(collectionUid);

            return new ResponseDTO(openApiSpec);
        } catch (Exception e) {
            throw new ApiException("Error during generation process: " + e.getMessage(), e, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (collectionUid != null) {
                try {
                    deleteCollection(collectionUid);
                } catch (Exception ex) {
                    log.error("Something failed: {}", ex.getMessage(), ex);
                }
            }
        }
    }

    private String createCollection(String collectionJson) {
        String url = apiProperties.getBaseUrl() + "/collections";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", apiProperties.getKey());

        HttpEntity<String> requestEntity = new HttpEntity<>(collectionJson, headers);

        ResponseEntity<String> responseEntity;
        try {
            responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
        } catch (Exception e) {
            throw new ApiException("Failed to create collection: " + e.getMessage(), e, HttpStatus.BAD_REQUEST);
        }

        if ((responseEntity.getStatusCode() == HttpStatus.OK || responseEntity.getStatusCode() == HttpStatus.CREATED)
                && responseEntity.getBody() != null) {
            try {
                JsonNode root = objectMapper.readTree(responseEntity.getBody());
                return root.path("collection").path("uid").asText();
            } catch (Exception e) {
                throw new ApiException("Error parsing response: " + e.getMessage(),
                        e, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            throw new ApiException(
                    "Request failed with status: " + responseEntity.getStatusCode(),
                    HttpStatus.valueOf(responseEntity.getStatusCodeValue())
            );
        }
    }

    private String generateOpenApiSpec(String collectionUid) {
        String url = apiProperties.getBaseUrl() + "/collections/" + collectionUid + "/transformations";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiProperties.getKey());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> responseEntity;
        try {
            responseEntity = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        } catch (Exception e) {
            throw new ApiException("Failed to generate OpenAPI spec: " + e.getMessage(),
                    e, HttpStatus.BAD_REQUEST);
        }

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            try {
                JsonNode root = objectMapper.readTree(responseEntity.getBody());
                String openApiSpec = root.path("output").asText();
                return openApiSpec;
            } catch (Exception e) {
                throw new ApiException("Error parsing response: " + e.getMessage(),
                        e, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            throw new ApiException(
                    "Request failed with status: " + responseEntity.getStatusCode(),
                    HttpStatus.valueOf(responseEntity.getStatusCodeValue())
            );
        }
    }

    private void deleteCollection(String collectionUid) {
        String url = apiProperties.getBaseUrl() + "/collections/" + collectionUid;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiProperties.getKey());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> responseEntity;
        try {
            responseEntity = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
        } catch (Exception e) {
            throw new ApiException("Failed to delete collection: " + e.getMessage(),
                    e, HttpStatus.BAD_REQUEST);
        }

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new ApiException(
                    "Request failed with status: " + responseEntity.getStatusCode(),
                    HttpStatus.valueOf(responseEntity.getStatusCodeValue())
            );
        }
    }
}
