package com.example.MyVolunteer_api.controller.auth;

import com.example.MyVolunteer_api.constants.Gender;
import com.example.MyVolunteer_api.constants.Role;
import com.example.MyVolunteer_api.dto.auth.*;
import com.example.MyVolunteer_api.model.auth.UserPrincipal;
import com.example.MyVolunteer_api.model.user.Organization;
import com.example.MyVolunteer_api.model.user.User;
import com.example.MyVolunteer_api.model.user.Volunteer;
import com.example.MyVolunteer_api.service.auth.JwtService;
import com.example.MyVolunteer_api.service.user.OrganizationService;
import com.example.MyVolunteer_api.service.user.UserService;
import com.example.MyVolunteer_api.service.user.VolunteerService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.InputMismatchException;

@Controller
@RequestMapping("user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private VolunteerService volunteerService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("userRequest") UserRegisterRequest userRequest, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("userRequest", userRequest);
            model.addAttribute("roles", Role.values());
            model.addAttribute("genders", Gender.values());
            return "auth/registerPage";
        }

        if (userService.findByEmail(userRequest.getEmail()) != null) {
            model.addAttribute("userRequest", new UserRegisterRequest());
            model.addAttribute("roles", Role.values());
            model.addAttribute("genders", Gender.values());
            model.addAttribute("error", "User already exists with this email.");
            return "auth/registerPage";
        }


        try {
            User user = null;

            switch (userRequest.getRole()) {
                case ORGANIZATION -> {
                    Organization organization = getOrganization(userRequest);
                    user = organizationService.createOrganization(organization);
                }
                case VOLUNTEER -> {
                    Volunteer volunteer = getVolunteer(userRequest);
                    user = volunteerService.createVolunteer(volunteer);
                }
            }
            user.setVerified(false);
            userService.createUser(user);
            userService.generateAndSendOTP(user);

            model.addAttribute("email", user.getEmail());
            return "auth/verifyOtpPage";
        } catch (Exception e) {
            model.addAttribute("userRequest", new UserRegisterRequest());
            model.addAttribute("roles", Role.values());
            model.addAttribute("genders", Gender.values());
            model.addAttribute("error", "An error occurred during registration.");
            return "auth/registerPage";
        }
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("userRequest") UserLoginRequest userRequest, BindingResult result, HttpServletResponse response, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("userRequest", userRequest);
            return "auth/loginPage";
        }

        User user = userService.findByEmail(userRequest.getEmail());

        if (user == null || !user.isVerified()) {
            model.addAttribute("error", "Please verify your OTP before logging in.");
            model.addAttribute("userRequest", new UserLoginRequest());
            return "auth/loginPage";
        }


        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userRequest.getEmail(), userRequest.getPassword()));

            if (authentication.isAuthenticated()) {
                String token = jwtService.generateToken(userRequest.getEmail());
                Cookie cookie = new Cookie("jwt", token);
                cookie.setHttpOnly(true);
                cookie.setSecure(true);
                cookie.setPath("/");
                cookie.setMaxAge(7 * 24 * 60 * 60);
                response.addCookie(cookie);
                return "redirect:/test/home";
            }
        } catch (AuthenticationException ex) {
            model.addAttribute("error", "password did not match");
            return "auth/loginPage";
        }
        model.addAttribute("error", "password did not match");
        return "auth/loginPage";
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);

        return "redirect:/test/home";
    }

    @PostMapping("/changePassword")
    public String changePassword(@Valid @ModelAttribute ChangePassDto changePassDto, RedirectAttributes redirectAttributes) {
        try {
            userService.changePassword(changePassDto);
            redirectAttributes.addFlashAttribute("message", "Successfully changed password, you can login now");
            return "redirect:/test/login";
        } catch (UsernameNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "User not found with this email");
            return "redirect:/test/changePassword";
        } catch (InputMismatchException e) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/test/changePassword";
        }
    }

    @PostMapping("/updateOrg")
    public String updateOrg(@Valid @ModelAttribute UpdateOrgAccDto updateOrgAccDto, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("orgErrors", result.getAllErrors());
            return "redirect:/test/orgAccUpdate";
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();

        String email = userDetails.getUsername();
        User user = userService.findByEmail(email);
        if (user.getRole() != Role.ORGANIZATION) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found");
            return "redirect:/test/home";
        }
        Organization organization = (Organization) user;
        organization.setName(updateOrgAccDto.getName());
        organization.setGstNumber(updateOrgAccDto.getGstNumber());
        organization.setLocation(updateOrgAccDto.getLocation());
        organization.setPhone(updateOrgAccDto.getPhone());
        organizationService.createOrganization(organization);
        redirectAttributes.addFlashAttribute("successMessage", "Account updated successfully!");
        return "redirect:/test/orgAccUpdate";
    }

    @PostMapping("/updateVol")
    public String updateVol(@Valid @ModelAttribute UpdateVolAccDto updateVolAccDto, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("volErrors", result.getAllErrors());
            return "redirect:/test/volAccUpdate";
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();

        String email = userDetails.getUsername();
        User user = userService.findByEmail(email);
        if (user.getRole() != Role.VOLUNTEER) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found");
            return "redirect:/test/home";
        }
        Volunteer volunteer = (Volunteer) user;
        volunteer.setName(updateVolAccDto.getName());
        volunteer.setPhone(updateVolAccDto.getPhone());
        volunteer.setSkills(updateVolAccDto.getSkills());
        volunteerService.createVolunteer(volunteer);
        redirectAttributes.addFlashAttribute("successMessage", "Account updated successfully!");
        return "redirect:/test/volAccUpdate";
    }


    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUser(HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();

        String email = userDetails.getUsername();

        userService.deleteUserByEmail(email);
        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);
        return ResponseEntity.ok("user deleted");
    }


    private static Volunteer getVolunteer(UserRegisterRequest userRequest) {
        Volunteer volunteer = new Volunteer();
        volunteer.setEmail(userRequest.getEmail());
        volunteer.setName(userRequest.getName());
        volunteer.setPassword(userRequest.getPassword());
        volunteer.setPhone(userRequest.getPhone());
        volunteer.setGender(userRequest.getGender());
        volunteer.setRole(userRequest.getRole());
        volunteer.setSkills(userRequest.getSkills());
        return volunteer;
    }

    private static Organization getOrganization(UserRegisterRequest userRequest) {
        Organization organization = new Organization();
        organization.setEmail(userRequest.getEmail());
        organization.setName(userRequest.getName());
        organization.setPassword(userRequest.getPassword());
        organization.setPhone(userRequest.getPhone());
        organization.setGender(userRequest.getGender());
        organization.setRole(userRequest.getRole());
        organization.setGstNumber(userRequest.getGstNumber());
        organization.setLocation(userRequest.getLocation());
        return organization;
    }

}