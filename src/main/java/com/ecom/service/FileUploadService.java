package com.ecom.service;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class FileUploadService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadFile(MultipartFile file) {
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("folder", "products"); // Optional

            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);

            String imageUrl = (String) result.get("secure_url");
          //  System.out.println("✅ Image uploaded to Cloudinary: " + imageUrl); // <-- Print the URL
            return imageUrl;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
