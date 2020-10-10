package top.gabin.demo.order.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    @GetMapping("/index")
    public String index() {
        return "test";
    }

    @GetMapping("/baidu")
    public String baidu() {
        return "baidu";
    }

}
