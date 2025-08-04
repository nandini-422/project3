package com.example.MyVolunteer_api.controller;

import com.example.MyVolunteer_api.constants.Gender;
import com.example.MyVolunteer_api.constants.OpportunityStatus;
import com.example.MyVolunteer_api.constants.Role;
import com.example.MyVolunteer_api.dto.auth.ChangePassDto;
import com.example.MyVolunteer_api.dto.auth.UserLoginRequest;
import com.example.MyVolunteer_api.dto.auth.UserRegisterRequest;
import com.example.MyVolunteer_api.dto.task.VolOppSaveDto;
import com.example.MyVolunteer_api.model.auth.UserPrincipal;
import com.example.MyVolunteer_api.model.user.Organization;
import com.example.MyVolunteer_api.model.user.User;
import com.example.MyVolunteer_api.model.user.Volunteer;
import com.example.MyVolunteer_api.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/test")
public class HomeController {

    @Autowired
    private UserService userService;

    @GetMapping("/home")
    public String homePage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        String email = null;
        if (principal instanceof UserPrincipal) {
            UserPrincipal userDetails = (UserPrincipal) principal;
            email = userDetails.getUsername();
        } else if (principal instanceof String) {
            email = (String) principal; // For cases like anonymous or basic auth
        }

        User user = (email != null) ? userService.findByEmail(email) : null;
        model.addAttribute("role", (user == null) ? "user" : user.getRole().toString());
        return "index";
    }

    @GetMapping("/login")
    public String loginRequestPage(Model model) {
        model.addAttribute("userRequest", new UserLoginRequest());
        return "auth/loginPage";
    }

    @GetMapping("/changePassword")
    public String changePasswordPage(Model model) {
        model.addAttribute("userRequest", new ChangePassDto());
        return "auth/changePassPage";
    }

    @GetMapping("/register")
    public String registerRequestPage(Model model) {
        UserRegisterRequest userRegisterRequest = new UserRegisterRequest();
        model.addAttribute("userRequest", userRegisterRequest);
        model.addAttribute("roles", Role.values());
        model.addAttribute("genders", Gender.values());
        return "auth/registerPage";
    }

    @GetMapping("/create")
    public String createVolOpp(Model model) {
        VolOppSaveDto volOpp = new VolOppSaveDto();
        model.addAttribute("statuses", OpportunityStatus.values());
        model.addAttribute("volOpp", volOpp);
        return "task/createVolOpp";
    }

    @GetMapping("/accDelete")
    public String accDelete() {
        return "auth/accDelete";
    }

    @GetMapping("/orgAccUpdate")
    public String orgAccUpdate(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        String email = null;
        if (principal instanceof UserPrincipal) {
            UserPrincipal userDetails = (UserPrincipal) principal;
            email = userDetails.getUsername();
        } else if (principal instanceof String) {
            email = (String) principal; // For cases like anonymous or basic auth
        }

        User user = (email != null) ? userService.findByEmail(email) : null;
        if (user == null || user.getRole() != Role.ORGANIZATION) {
            return "redirect:/test/home";
        }
        Organization organization = (Organization) user;
        model.addAttribute("organization", organization);
        return "auth/orgUpdateAcc";
    }

    @GetMapping("/volAccUpdate")
    public String volAccUpdate(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        String email = null;
        if (principal instanceof UserPrincipal) {
            UserPrincipal userDetails = (UserPrincipal) principal;
            email = userDetails.getUsername();
        } else if (principal instanceof String) {
            email = (String) principal;
        }

        User user = (email != null) ? userService.findByEmail(email) : null;
        if (user == null || user.getRole() != Role.VOLUNTEER) {
            return "redirect:/test/home";
        }
        Volunteer volunteer = (Volunteer) user;
        model.addAttribute("volunteer", volunteer);
        return "auth/volUpdateAcc";
    }

    @GetMapping("/profile")
    public String getProfile(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        String email = null;
        if (principal instanceof UserPrincipal) {
            UserPrincipal userDetails = (UserPrincipal) principal;
            email = userDetails.getUsername();
        } else if (principal instanceof String) {
            email = (String) principal;
        }

        User user = (email != null) ? userService.findByEmail(email) : null;
        if (user == null) {
            return "redirect:/test/home";
        }
        if (user.getRole() == Role.ORGANIZATION) {
            Organization organization = (Organization) user;
            model.addAttribute("user", organization);
        } else if (user.getRole() == Role.VOLUNTEER) {
            Volunteer volunteer = (Volunteer) user;
            model.addAttribute("user", volunteer);
        }
        return "auth/profile";
    }


}
