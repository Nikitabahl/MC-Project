package asu.brainnet.service.impl;

import SVM.Class1;
import asu.brainnet.model.BrainSignalsInfo;
import asu.brainnet.service.ClientServiceInterface;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.mathworks.toolbox.javabuilder.MWArray;
import com.mathworks.toolbox.javabuilder.MWException;
import com.mathworks.toolbox.javabuilder.MWNumericArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ClientServiceImpl implements ClientServiceInterface {

    private String awsS3AudioBucket;
    private AmazonS3 amazonS3;
    private MongoTemplate mongoTemplate;
    private static final String authenticateFileName = "authenticateFile.edf";
    private static final Logger logger = LoggerFactory.getLogger(ClientServiceImpl.class);

    public ClientServiceImpl(Region awsRegion, AWSCredentialsProvider awsCredentialsProvider, String awsS3AudioBucket,
                             MongoTemplate mongoTemplate) {

        this.amazonS3 = AmazonS3ClientBuilder.standard()
                .withCredentials(awsCredentialsProvider)
                .withRegion(awsRegion.getName()).build();
        this.awsS3AudioBucket = awsS3AudioBucket;
        this.mongoTemplate = mongoTemplate;
    }

    @Async
    public void uploadFileToS3Bucket(MultipartFile multipartFile, boolean enablePublicReadAccess) {

        String fileName = multipartFile.getOriginalFilename();

        try {
            //creating the file in the server (temporarily)
            File file = new File(fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(multipartFile.getBytes());
            fos.close();

            PutObjectRequest putObjectRequest = new PutObjectRequest(this.awsS3AudioBucket, fileName, file);

            if (enablePublicReadAccess) {
                putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead);
            }
            this.amazonS3.putObject(putObjectRequest);
            //removing the file created in the server
            file.delete();
        } catch (IOException | AmazonServiceException ex) {
            logger.error("error [" + ex.getMessage() + "] occurred while uploading [" + fileName + "] ");
        }
    }

    @Async
    public void deleteFileFromS3Bucket(String fileName) {
        try {
            amazonS3.deleteObject(new DeleteObjectRequest(awsS3AudioBucket, fileName));
        } catch (AmazonServiceException ex) {
            logger.error("error [" + ex.getMessage() + "] occurred while removing [" + fileName + "] ");
        }
    }

    public BrainSignalsInfo uploadDataToMongo(String userName, MultipartFile multipartFile) {

        BrainSignalsInfo brainSignalsInfo = new BrainSignalsInfo();

        try {
            brainSignalsInfo.setUserName(userName);
            brainSignalsInfo.setFileContent(readFile(multipartFile));

            brainSignalsInfo = mongoTemplate.insert(brainSignalsInfo);

        } catch (Exception e) {
            logger.error("Unable to insert data into mongo", e);
        }
        return brainSignalsInfo;
    }

    @Override
    public boolean authenticateUser(String userName, MultipartFile multipartFile) {

        boolean isAuthenticated = false;
        String fileName = multipartFile.getOriginalFilename();

        Query query = new Query();
        query.addCriteria(Criteria.where("user_name").is(userName));
        BrainSignalsInfo brainSignalsInfo = mongoTemplate.findOne(query, BrainSignalsInfo.class);

        if (null == brainSignalsInfo) {
            throw new RuntimeException("No file present for user name - " + userName);
        }

        try {

            File file = new File(fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(multipartFile.getBytes());
            fos.close();

            File authenticationFile = new File(authenticateFileName);
            FileOutputStream authenticationFileOS = new FileOutputStream(authenticationFile);
            authenticationFileOS.write(brainSignalsInfo.getFileContent().getBytes());
            authenticationFileOS.close();

            isAuthenticated = authenticateFiles(fileName, authenticateFileName);

        } catch (Exception e) {
            logger.error("Unable to authenticate the user", e);
        }
        return isAuthenticated;
    }

    private boolean authenticateFiles(String incomingFile, String registeredFile) throws MWException {

        File f = new File(incomingFile);

        if (f.exists()) {

            Object[] resultSum = null;

            try {

                Class1 svm = new Class1();

                resultSum = svm.SVM(1, incomingFile, registeredFile);

                MWArray javaSum = (MWNumericArray) resultSum[0];

                double[][] total = (double[][]) javaSum.toArray();

                int finalResult = (int) total[0][0];


                return finalResult == 1;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            return false;
        }
        return false;
    }

    private static String readFile(MultipartFile file) throws IOException {

        InputStream fileStream = file.getInputStream();

        byte[] buffer = new byte[3500000];
        int size = fileStream.read(buffer);
        return new String(buffer, 0, size);
    }
}