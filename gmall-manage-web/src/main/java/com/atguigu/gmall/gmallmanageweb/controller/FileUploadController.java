package com.atguigu.gmall.gmallmanageweb.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@CrossOrigin
@RestController
public class FileUploadController {

    @Value("${fileServer.url}")
    private String fileUrl;

    @RequestMapping(value = "fileUpload")
    public String fileUpload(@RequestParam("file") MultipartFile file) throws IOException, MyException {
        String imgUrl = fileUrl;
        if(file!=null){
            String configFile = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init(configFile);
            TrackerClient trackerClient=new TrackerClient();
            TrackerServer trackerServer=trackerClient.getConnection();
            StorageClient storageClient=new StorageClient(trackerServer,null);
            //String orginalFilename="e://img/timg11.jpg";
            //http://192.168.213.221/group1/M00/00/00/wKjV3V1z3wqAMZaWAAEVp8Yu0cs530.jpg
            //String orginalFilename = "";
            String orginalFilename = file.getOriginalFilename();
            String suffixName = StringUtils.substringAfterLast(orginalFilename, ".");
            String[] upload_file = storageClient.upload_file(
                    file.getBytes(), suffixName, null);
            for (int i = 0; i < upload_file.length; i++) {
                String s = upload_file[i];
                System.out.println("s = " + s);
//                s = group1
//                s = M00/00/00/wKjV3V1z3wqAMZaWAAEVp8Yu0cs530.jpg
                imgUrl+="/"+s;
            }
        }
        return imgUrl;
    }
}
