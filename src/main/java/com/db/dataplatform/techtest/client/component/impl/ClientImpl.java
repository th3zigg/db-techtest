package com.db.dataplatform.techtest.client.component.impl;

import com.db.dataplatform.techtest.client.api.model.DataEnvelope;
import com.db.dataplatform.techtest.client.component.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

import java.util.*;

/**
 * Client code does not require any test coverage
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientImpl implements Client {

    public static final String URI_PUSHDATA = "http://localhost:8090/dataserver/pushdata";
    public static final UriTemplate URI_GETDATA = new UriTemplate("http://localhost:8090/dataserver/data/{blockType}");
    public static final UriTemplate URI_PATCHDATA = new UriTemplate("http://localhost:8090/dataserver/update/{name}/{newBlockType}");

    private RestTemplate restTemplate;

    @Autowired
    public ClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean pushData(DataEnvelope dataEnvelope, String checkSum) {
        log.info("Pushing data {} to {}", dataEnvelope.getDataHeader().getName(), URI_PUSHDATA);
        if (checkSum == null || checkSum.isEmpty()) {
            throw new IllegalArgumentException("Empty or missing client checksum");
        }
        try {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("X-Content-MD5", checkSum);
            final HttpEntity<DataEnvelope> request = new HttpEntity<>(dataEnvelope, headers);

            ResponseEntity<Boolean> pushDataResponse = restTemplate.postForEntity(URI_PUSHDATA, request, Boolean.class);

            if (pushDataResponse.getStatusCode().is2xxSuccessful()) {
                log.info("Pushing data returned with response: {}", pushDataResponse.getBody());
                return pushDataResponse.getBody() != null && pushDataResponse.getBody();
            } else {
                log.warn("Pushing data to {} return status code {}", URI_PUSHDATA, pushDataResponse.getStatusCodeValue());
            }
        } catch (RestClientException ex) {
            log.error("Pushing data to {} returned exception", URI_PUSHDATA, ex);
        }
        return false;
    }

    @Override
    public List<DataEnvelope> getData(String blockType) {
        log.info("Query for data with header block type {}", blockType);

        ResponseEntity<DataEnvelope[]> response = restTemplate.getForEntity(URI_GETDATA.expand(blockType), DataEnvelope[].class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return Arrays.asList(response.getBody());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean updateData(String blockName, String newBlockType) {
        log.info("Updating blocktype to {} for block with name {}", newBlockType, blockName);
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("name", blockName);
        uriVariables.put("newBlockType", newBlockType);
        Boolean response = restTemplate.patchForObject(URI_PATCHDATA.expand(uriVariables), null, Boolean.class);
        return response != null && response;
    }
}
