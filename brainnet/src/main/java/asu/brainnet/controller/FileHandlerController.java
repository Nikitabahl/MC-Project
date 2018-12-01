package asu.brainnet.controller;

import asu.brainnet.model.BrainSignalsInfo;
import asu.brainnet.service.ClientServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileHandlerController {

    private final ClientServiceInterface amazonS3ClientService;

    public FileHandlerController(ClientServiceInterface amazonS3ClientService) {
        this.amazonS3ClientService = amazonS3ClientService;
    }

    @PostMapping
    public Map<String, String> uploadFileToS3(@RequestPart(value = "file") MultipartFile file) {
        this.amazonS3ClientService.uploadFileToS3Bucket(file, true);

        Map<String, String> response = new HashMap<>();
        response.put("message", "file [" + file.getOriginalFilename() + "] uploading request submitted successfully.");

        return response;
    }

    @DeleteMapping
    public Map<String, String> deleteFile(@RequestParam("file_name") String fileName) {
        this.amazonS3ClientService.deleteFileFromS3Bucket(fileName);

        Map<String, String> response = new HashMap<>();
        response.put("message", "file [" + fileName + "] removing request submitted successfully.");

        return response;
    }

    @PostMapping("/authenticate")
    public boolean authenticateUser(
            @RequestParam(value = "userName") String userName,
            @RequestPart(value = "file") MultipartFile file) {

        return amazonS3ClientService.authenticateUser(userName, file);
    }

    @PostMapping(path = "/mongo")
    public void uploadFileToMongo(
            @RequestParam(value = "userName") String userName,
            @RequestPart(value = "file") MultipartFile file) throws IOException {

        amazonS3ClientService.uploadDataToMongo(userName, file);
    }
}