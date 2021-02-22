package com.db.dataplatform.techtest.server.component.impl;

import com.db.dataplatform.techtest.server.api.model.DataBody;
import com.db.dataplatform.techtest.server.api.model.DataEnvelope;
import com.db.dataplatform.techtest.server.persistence.BlockTypeEnum;
import com.db.dataplatform.techtest.server.persistence.model.DataBodyEntity;
import com.db.dataplatform.techtest.server.persistence.model.DataHeaderEntity;
import com.db.dataplatform.techtest.server.service.DataBodyService;
import com.db.dataplatform.techtest.server.component.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerImpl implements Server {

    private final DataBodyService dataBodyService;
    private final ModelMapper modelMapper;

    /**
     * @param envelope
     * @param checkSum
     * @return true if there is a match with the client provided checksum.
     */
    @Override
    public boolean saveDataEnvelope(DataEnvelope envelope, String checkSum) throws NoSuchAlgorithmException {
        if (checkSum == null || checkSum.isEmpty()) {
            return false;
        }
        String computedChecksum = computeMd5(envelope.getDataBody());
        if (!checkSum.equals(computedChecksum)) {
            log.warn("provided checksum {} does not match computed checksum {}", checkSum, computedChecksum);
            return false;
        }

        // Save to persistence.
        persist(envelope, computedChecksum);

        log.info("Data persisted successfully, data name: {}", envelope.getDataHeader().getName());
        return true;
    }

    @Override
    public List<DataEnvelope> getDataEnvelopes(BlockTypeEnum blockTypeEnum) {
        return null;
    }

    /**
     * @param name the name of the Data block
     * @param blockType the block type
     * @return true if the update was successful
     */
    @Override
    public boolean updateDataBlockType(String name, String blockType) {
        Optional<DataBodyEntity> dataBlockOptional = dataBodyService.getDataByBlockName(name);
        if (dataBlockOptional.isPresent()) {
            DataBodyEntity dataBlock = dataBlockOptional.get();
            DataHeaderEntity headerEntity = dataBlock.getDataHeaderEntity();
            headerEntity.setBlocktype(BlockTypeEnum.valueOf(blockType));
            dataBlock.setDataHeaderEntity(headerEntity);
            saveData(dataBlock);
            return true;
        }

        return false;
    }

    private String computeMd5(DataBody dataBody) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(dataBody.getDataBody().getBytes());
        return DatatypeConverter.printHexBinary(digest);
    }

    private void persist(DataEnvelope envelope, final String computedChecksum) {
        log.info("Persisting data with attribute name: {}", envelope.getDataHeader().getName());
        DataHeaderEntity dataHeaderEntity = modelMapper.map(envelope.getDataHeader(), DataHeaderEntity.class);

        DataBodyEntity dataBodyEntity = modelMapper.map(envelope.getDataBody(), DataBodyEntity.class);
        dataBodyEntity.setDataHeaderEntity(dataHeaderEntity);
        dataBodyEntity.setDataBodyChecksum(computedChecksum);

        saveData(dataBodyEntity);
    }

    private void saveData(DataBodyEntity dataBodyEntity) {
        dataBodyService.saveDataBody(dataBodyEntity);
    }
}
