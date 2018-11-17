package asu.brainnet.service;

import asu.brainnet.model.BrainSignalsInfo;
import org.springframework.web.multipart.MultipartFile;

public interface ClientServiceInterface {

    void uploadFileToS3Bucket(MultipartFile multipartFile, boolean enablePublicReadAccess);

    void deleteFileFromS3Bucket(String fileName);

    BrainSignalsInfo uploadDataToMongo(String userName, MultipartFile multipartFile);
}

