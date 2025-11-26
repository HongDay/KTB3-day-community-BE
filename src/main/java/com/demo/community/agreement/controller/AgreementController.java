package com.demo.community.agreement.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AgreementController {

    @Value("${fronturl}")
    private String frontUrl;

    @GetMapping("/agreement")
    public String agreement(Model model) {
        model.addAttribute("name", "홍대의");
        model.addAttribute("nextPage", frontUrl + "/public/pages/posts/posts.html");
        return "agreement";
    }
}
