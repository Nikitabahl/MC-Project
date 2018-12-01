package asu.brainnet.service;

import asu.brainnet.model.BrainSignalsInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ClientServiceInterface {

    void uploadFileToS3Bucket(MultipartFile multipartFile, boolean enablePublicReadAccess);

    void deleteFileFromS3Bucket(String fileName);

    boolean uploadDataToMongo(String userName, MultipartFile multipartFile) throws IOException;

    boolean authenticateUser(String userName, MultipartFile multipartFile);
}

