package com.db.dataplatform.techtest.server.api.controller;

import com.db.dataplatform.techtest.server.api.model.DataEnvelope;
import com.db.dataplatform.techtest.server.component.Server;
import com.db.dataplatform.techtest.server.persistence.BlockTypeEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Slf4j
@Controller
@RequestMapping("/dataserver")
@RequiredArgsConstructor
@Validated
public class ServerController {

    private final Server server;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private static final String HADOOP_DATAPUSH_ENDPOINT = "http://localhost:8090/hadoopserver/pushbigdata";

    @PostMapping(value = "/pushdata", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> pushData(@Valid @RequestBody DataEnvelope dataEnvelope, @RequestHeader(value = "X-Content-MD5") String checkSum) throws IOException, NoSuchAlgorithmException {
        if (checkSum == null || checkSum.isEmpty()) {
            log.error("Missing checksum in header");
            return ResponseEntity.badRequest().body(Boolean.FALSE);
        }

        log.info("Data envelope received: {}", dataEnvelope.getDataHeader().getName());
        boolean checksumPass = server.saveDataEnvelope(dataEnvelope, checkSum);

        if (checksumPass) {
            try {
                pushToHadoop(dataEnvelope);
                log.info("Returned from call of Hadoop");
            } catch (HttpServerErrorException e) {
                log.error("Timeout pushing to big data", e);
            }
        }

        log.info("Data envelope persisted. Attribute name: {}", dataEnvelope.getDataHeader().getName());
        return ResponseEntity.ok(checksumPass);
    }

    @GetMapping(value = "/data/{blockType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DataEnvelope>> getData(@PathVariable String blockType) {
        try {
            BlockTypeEnum blockTypeEnum = BlockTypeEnum.valueOf(blockType);
            return ResponseEntity.ok(server.getDataEnvelopes(blockTypeEnum));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PatchMapping(value = "/update/{name}/{newBlockType}")
    public ResponseEntity<Boolean> updateData(@PathVariable String name, @PathVariable String newBlockType) {
        boolean isUpdateSuccessful = server.updateDataBlockType(name, newBlockType);
        return ResponseEntity.ok(isUpdateSuccessful);
    }

    @Retryable(value = {HttpServerErrorException.class}, maxAttempts = 5, backoff = @Backoff(200))
    void pushToHadoop(DataEnvelope dataEnvelope) throws JsonProcessingException, HttpServerErrorException {
        String dataEnvelopeJson = objectMapper.writeValueAsString(dataEnvelope);
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(dataEnvelopeJson, headers);

        log.info("Calling {} for big data push", HADOOP_DATAPUSH_ENDPOINT);
        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(HADOOP_DATAPUSH_ENDPOINT, request, Void.class);
            log.info("Data push to Hadoop returned with status code {}", response.getStatusCode());
        } catch (HttpServerErrorException ex) {
            if (ex.getStatusCode().value() == 504) {
                log.info("Rethrowing in the hope of retry");
                throw ex;
            }
        }
    }
}
