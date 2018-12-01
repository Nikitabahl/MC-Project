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
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSDownloadByNameOptions;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

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

    public boolean uploadDataToMongo(String userName, MultipartFile multipartFile) throws IOException {

        String fileName = multipartFile.getOriginalFilename();
        File file = new File(fileName);
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(multipartFile.getBytes());
        fos.close();

        boolean isInsert = false;

        try {

            MongoDatabase myDatabase = mongoTemplate.getDb();
            GridFSBucket gridFSBucket = GridFSBuckets.create(myDatabase);

            InputStream streamToUploadFrom = new FileInputStream(new File(fileName));

            GridFSUploadOptions options = new GridFSUploadOptions()
                    .metadata(new Document("type", "user-data"));

            gridFSBucket.uploadFromStream(fileName, streamToUploadFrom, options);

            BrainSignalsInfo brainSignalsInfo = new BrainSignalsInfo();
            brainSignalsInfo.setUserName(userName);
            brainSignalsInfo.setFileName(fileName);

            mongoTemplate.insert(brainSignalsInfo);
            isInsert = true;

        } catch (FileNotFoundException e){
            logger.error("File not found", e);
        }
        return isInsert;
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

            MongoDatabase myDatabase = mongoTemplate.getDb();
            GridFSBucket gridFSBucket = GridFSBuckets.create(myDatabase);

            FileOutputStream streamToDownloadTo = new FileOutputStream(authenticateFileName);
            GridFSDownloadByNameOptions downloadOptions = new GridFSDownloadByNameOptions().revision(0);
            gridFSBucket.downloadToStreamByName(brainSignalsInfo.getFileName(), streamToDownloadTo, downloadOptions);
            streamToDownloadTo.close();

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