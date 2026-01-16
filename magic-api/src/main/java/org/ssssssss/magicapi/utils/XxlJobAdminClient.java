package org.ssssssss.magicapi.utils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


//@Component
//public class XxlJobAdminClient {
//
//    private final XxlJobAdminProperties properties;
//    private final RestTemplate restTemplate;
//
//    public XxlJobAdminClient(XxlJobAdminProperties properties, RestTemplate restTemplate) {
//        this.properties = properties;
//        this.restTemplate = new RestTemplate();
//    }
//
//    public String insertJob(Object jobInfo) {
//        // 拼接完整 URL
//        String url = properties.getAddresses() + "/jobinfo/insert";
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        // 如果 admin 设置了 token
//        if (properties.getAccessToken() != null && !properties.getAccessToken().isEmpty()) {
//            headers.set("XXL-JOB-ACCESS-TOKEN", properties.getAccessToken());
//        }
//
//        HttpEntity<Object> requestEntity = new HttpEntity<>(jobInfo, headers);
//
//        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
//        return response.getBody();
//    }
//}

@Component
public class XxlJobAdminClient {

    private final XxlJobAdminProperties properties;
    private final RestTemplate restTemplate;

    public XxlJobAdminClient(XxlJobAdminProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        // 如果外部传入的 restTemplate 为 null，使用默认的
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
    }

    public String insertJob(Object jobInfo) {
        return post("/jobinfo/insert", jobInfo);
    }

    public String updateJob(Object jobInfo) {
        return post("/jobinfo/update", jobInfo);
    }

    private String post(String path, Object jobInfo) {
        String url = properties.getAddresses() + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getAccessToken() != null && !properties.getAccessToken().isEmpty()) {
            headers.set("XXL-JOB-ACCESS-TOKEN", properties.getAccessToken());
        }

        HttpEntity<Object> requestEntity = new HttpEntity<>(jobInfo, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        return response.getBody();
    }
}