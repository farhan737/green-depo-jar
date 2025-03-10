package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);
    
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            
            logger.error("Error with status code {} occurred. Path: {}", statusCode, 
                    request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
            
            if (exception != null) {
                logger.error("Exception: ", (Throwable) exception);
            }
            
            model.addAttribute("status", statusCode);
            
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("error", "Page Not Found");
                model.addAttribute("message", "The page you are looking for does not exist.");
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                model.addAttribute("error", "Access Denied");
                model.addAttribute("message", "You don't have permission to access this resource.");
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                model.addAttribute("error", "Internal Server Error");
                model.addAttribute("message", errorMessage != null ? errorMessage : "Something went wrong on our end. Please try again later.");
            } else {
                model.addAttribute("error", "Error");
                model.addAttribute("message", "An unexpected error occurred.");
            }
        } else {
            model.addAttribute("error", "Unknown Error");
            model.addAttribute("message", "An unexpected error occurred.");
        }
        
        return "error";
    }
}
