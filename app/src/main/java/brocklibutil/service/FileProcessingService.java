package brocklibutil.service;

import static brocklibutil.helper.Helper.AUTH_TOKEN_KEY;
import static brocklibutil.helper.Helper.AUTH_TOKEN_PREFIX;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.lang.Math;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.lang.Thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import brocklibutil.controller.LibinsightController;
import brocklibutil.domain.EzproxyLibinsightFeed;

@Service
public class FileProcessingService {

    Logger logger = LoggerFactory.getLogger(FileProcessingService.class);

    static final String DATASET_ID = "12230";
    static final String LIBINSIGHT_STORE_URL = "https://brocku.libinsight.com/post/v1.0/custom/" + DATASET_ID
            + "/type/1/save";

    @Autowired
    private LibinsightController libinsightController;

    public String processFiles(List<MultipartFile> files) throws JsonProcessingException {
        Map<String, Map<String, Long>> map = new HashMap<>();
        Pattern pattern = Pattern.compile(".+\\bhttp[s]?://\\b(.+?)[/\s]+.+");

        map = files.stream()
                .flatMap(file -> {
                    try {
                        InputStreamReader streamReader = new InputStreamReader(file.getInputStream(),
                                StandardCharsets.UTF_8);
                        BufferedReader reader = new BufferedReader(streamReader);

                        return reader.lines();

                    } catch (Exception e) {
                        logger.error("File processing failed: " + file.getOriginalFilename(), e);
                        return new ArrayList<String>().stream();
                    }
                }).map(str -> {
                    Matcher matcher = pattern.matcher(str);

                    if (matcher.matches()) {
                        String url = matcher.group(1);

                        try {
                            String decodedUrl = UriUtils.decode(url, "UTF-8");

                            String dateStr = str.split("\\]")[0].split("\\[")[1];

                            String dateStrNew = LocalDateTime
                                    .parse(dateStr, DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss xxxx"))
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH"));

                            return Arrays.asList(dateStrNew + ":00:00", decodedUrl.split("/")[0].split("\\?")[0]);
                        
                        } catch (Exception ex) {
                            return Arrays.asList("", "");
                        }
                    }

                    return Arrays.asList("", "");
                })
                .collect(Collectors.groupingBy(arr -> arr.get(0),
                        Collectors.groupingBy(arr -> arr.get(1), Collectors.counting())));

        List<EzproxyLibinsightFeed> feed = map.entrySet()
                .stream()
                .flatMap(e -> e.getValue()
                        .entrySet()
                        .stream()
                        .filter(val -> !"".equals(e.getKey()) && !"".equals(val.getKey()))
                        .map(val -> {
                            return new EzproxyLibinsightFeed(e.getKey(), val.getKey(), val.getValue());
                        }))
                .collect(Collectors.toList());

        String libinsightAccessToken = libinsightController.fetchLibinsightToken().getBody();

        if (libinsightAccessToken == null || libinsightAccessToken.isEmpty()) {
            return "Failed";
        }

        String response = "";

        final int blockSize = 200;

        for (int i = 0; i < feed.size(); i += blockSize) {
            ObjectMapper mapper = new ObjectMapper();
            String payload = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(feed.subList(i, Math.min(i + blockSize, feed.size())));

            response += saveChunk(payload, libinsightAccessToken);
            logger.info("Chunk saved: " + (((i + 1) / blockSize) + 1) + "/" + ((feed.size() / blockSize) + 1));
        }

        return response;
    }

    private String saveChunk(String payload, String libinsightAccessToken) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set(AUTH_TOKEN_KEY, AUTH_TOKEN_PREFIX + libinsightAccessToken);

        HttpEntity<String> entity = new HttpEntity<String>(payload, httpHeaders);

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate
                .exchange(LIBINSIGHT_STORE_URL, HttpMethod.POST, entity, String.class)
                .getBody();
    }
}

