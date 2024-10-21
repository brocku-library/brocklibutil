package brocklibutil.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;

import brocklibutil.domain.EzProxyMultipartFiles;
import brocklibutil.service.FileProcessingService;

@Controller
@RequestMapping("/ezproxy")
public class EzProxyLibinsightController {

    Logger logger = LoggerFactory.getLogger(EzProxyLibinsightController.class);

    @Autowired
    private FileProcessingService fileProcessingService;

    // @GetMapping
    public String ezproxyDump(@RequestParam String pin, Model model) {
        model.addAttribute("fileModel", new EzProxyMultipartFiles());

        if (!"wubalubadubdub".equals(pin)) {
            return null;
        }

        return "ezproxy";
    }

    // @PostMapping
    public String getFiles(@ModelAttribute EzProxyMultipartFiles fileModel, RedirectAttributes redirectAttributes)
            throws JsonProcessingException {

        if (fileModel != null && fileModel.getFiles() != null) {
            String response = fileProcessingService.processFiles(fileModel.getFiles());
            redirectAttributes.addFlashAttribute("msg", response);
        } else {
            redirectAttributes.addFlashAttribute("msg", "Failed. Talk to the developer.");
        }

        return "redirect:/ezproxy?pin=wubalubadubdub";
    }

    @PostMapping("/upload")
    public ResponseEntity<String> getSingleFile(@ModelAttribute MultipartFile file) throws JsonProcessingException {
        if (file != null) {
            String response = fileProcessingService.processFiles(List.of(file));
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            logger.error("File doesn't exist");
        }

        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
}