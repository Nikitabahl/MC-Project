package asu.brainnet;

import org.springframework.web.multipart.MultipartFile;

public interface ClientServiceInterface
{
    void uploadFileToS3Bucket(MultipartFile multipartFile, boolean enablePublicReadAccess);

    void deleteFileFromS3Bucket(String fileName);
}

