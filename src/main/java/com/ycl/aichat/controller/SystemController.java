package com.ycl.aichat.controller;

import com.ycl.aichat.config.WebSocketServer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class SystemController {
    @GetMapping("/index/{userId}")
    public String index(@PathVariable("userId") String userId, Model model) {
        model.addAttribute("userId", userId);
        return "index";
    }

    @ResponseBody
    @GetMapping("/socket/push/{cid}")
    public Map<String, Object> pushToWeb(@PathVariable("cid") String cid, String message) {
        Map<String, Object> result = new HashMap(16);
        try {
            WebSocketServer.sendInfo(message, cid);
            result.put("code", 200);
            result.put("msg", "success");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}