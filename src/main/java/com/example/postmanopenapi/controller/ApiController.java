package com.example.postmanopenapi.controller;

import com.example.postmanopenapi.dto.ResponseDTO;
import com.example.postmanopenapi.dto.RequestDTO;
import com.example.postmanopenapi.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/postman")
@CrossOrigin(origins = "http://localhost:4200")
public class ApiController {

    @Autowired
    private ApiService apiService;

    @PostMapping("/convert")
    public ResponseEntity<ResponseDTO> convertCollectionToOpenApi(
            @RequestBody RequestDTO dto
    ) {
        String collectionJson = dto.getCollectionJson();
        ResponseDTO openApiResponse = apiService.generate(collectionJson);
        return ResponseEntity.ok(openApiResponse);
    }
}
