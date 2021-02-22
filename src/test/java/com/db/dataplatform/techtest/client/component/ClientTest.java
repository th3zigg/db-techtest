package com.db.dataplatform.techtest.client.component;

import com.db.dataplatform.techtest.TestDataHelper;
import com.db.dataplatform.techtest.api.controller.ServerControllerComponentTest;
import com.db.dataplatform.techtest.client.api.model.DataBody;
import com.db.dataplatform.techtest.client.api.model.DataEnvelope;
import com.db.dataplatform.techtest.client.api.model.DataHeader;
import com.db.dataplatform.techtest.client.component.impl.ClientImpl;
import com.db.dataplatform.techtest.server.persistence.BlockTypeEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.db.dataplatform.techtest.Constant.DUMMY_DATA;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClientTest {
    @Mock
    private RestTemplate restTemplate;

    private Client client;

    private DataEnvelope testDataEnvelope;
    private String checksum = "checksum";
    private HttpEntity<DataEnvelope> httpRequestEntity;


    @Before
    public void setUp() {
        client = new ClientImpl(restTemplate);

        DataBody dataBody = new DataBody(DUMMY_DATA);
        DataHeader dataHeader = new DataHeader("HEADER_NAME", BlockTypeEnum.BLOCKTYPEA);
        testDataEnvelope = new DataEnvelope(dataHeader, dataBody);

        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-MD5", checksum);
        httpRequestEntity = new HttpEntity<>(testDataEnvelope, headers);
    }

    @Test
    public void whenDataIsPushedWithValidChecksum_thenReturnSuccess() throws Exception {
        when(restTemplate.postForEntity(ServerControllerComponentTest.URI_PUSHDATA, httpRequestEntity, Boolean.class))
                .thenReturn(new ResponseEntity<>(Boolean.TRUE, HttpStatus.OK));
        boolean isPushDataSuccessful = client.pushData(testDataEnvelope, checksum);
        assertTrue(isPushDataSuccessful);
    }

    @Test
    public void whenDataIsPushedWithInvalidChecksum_thenReturnFailure() throws Exception {
        when(restTemplate.postForEntity(ServerControllerComponentTest.URI_PUSHDATA, httpRequestEntity, Boolean.class))
                .thenReturn(new ResponseEntity<>(Boolean.FALSE, HttpStatus.OK));
        boolean isPushDataSuccessful = client.pushData(testDataEnvelope, checksum);
        assertFalse(isPushDataSuccessful);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenDataIsPushedWithEmptyChecksum_thenReturnFailure() throws Exception {
        boolean isPushDataSuccessful = client.pushData(testDataEnvelope, "");
        assertFalse(isPushDataSuccessful);
    }

    @Test
    public void whenDataIsPushedWithNullChecksum_thenReturnFailure() throws Exception {
        when(restTemplate.postForEntity(ServerControllerComponentTest.URI_PUSHDATA, httpRequestEntity, Boolean.class))
                .thenReturn(new ResponseEntity<>(Boolean.FALSE, HttpStatus.OK));
        boolean isPushDataSuccessful = client.pushData(testDataEnvelope, checksum);
        assertFalse(isPushDataSuccessful);
    }

    @Test
    public void whenDataIsPushed_thenReturnHttpStatus500() throws Exception {
        when(restTemplate.postForEntity(ServerControllerComponentTest.URI_PUSHDATA, httpRequestEntity, Boolean.class))
                .thenReturn(new ResponseEntity<>(Boolean.FALSE, HttpStatus.INTERNAL_SERVER_ERROR));
        boolean isPushDataSuccessful = client.pushData(testDataEnvelope, checksum);
        assertFalse(isPushDataSuccessful);
    }

    @Test
    public void whenDataIsPushed_thenThrowNoSuchAlgorithmException() throws Exception {
        when(restTemplate.postForEntity(ServerControllerComponentTest.URI_PUSHDATA, httpRequestEntity, Boolean.class)).thenThrow(new RestClientException("Error"));
        boolean isPushDataSuccessful = client.pushData(testDataEnvelope, checksum);
        assertFalse(isPushDataSuccessful);
    }

    @Test
    public void whenDataIsRequestedForGiveBlockType_thenReturnAllDataBlocksThatMatch() {
        String blockType = "blocktypea";
        DataEnvelope[] expected = new DataEnvelope[]{testDataEnvelope};
        when(restTemplate.getForEntity(ServerControllerComponentTest.URI_GETDATA.expand(blockType), DataEnvelope[].class))
                .thenReturn(new ResponseEntity<>(expected, HttpStatus.OK));
        List<DataEnvelope> data = client.getData(blockType);
        assertTrue(data.contains(testDataEnvelope));
    }

    @Test
    public void updateData() throws Exception {
        String blockName = "blockName";
        String newBlockType = "blocktypea";
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("name", blockName);
        uriVariables.put("newBlockType", newBlockType);

        when(restTemplate.patchForObject(ServerControllerComponentTest.URI_PATCHDATA.expand(uriVariables), null, Boolean.class))
                .thenReturn(Boolean.TRUE);
        boolean isUpdateSuccessful = client.updateData(blockName, newBlockType);
        assertTrue(isUpdateSuccessful);
    }
}