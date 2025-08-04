package com.example.MyVolunteer_api.controller.task;

import com.example.MyVolunteer_api.constants.Role;
import com.example.MyVolunteer_api.constants.SignUpStatus;
import com.example.MyVolunteer_api.dto.task.SignupForTaskDto;
import com.example.MyVolunteer_api.dto.task.SignupForTaskRequest;
import com.example.MyVolunteer_api.dto.task.SignupForVolDto;
import com.example.MyVolunteer_api.dto.task.VolunteerOpportunitiesDTO;
import com.example.MyVolunteer_api.model.auth.UserPrincipal;
import com.example.MyVolunteer_api.model.task.TaskSignups;
import com.example.MyVolunteer_api.model.task.VolunteerOpportunities;
import com.example.MyVolunteer_api.model.user.User;
import com.example.MyVolunteer_api.model.user.Volunteer;
import com.example.MyVolunteer_api.service.task.TaskSignupsService;
import com.example.MyVolunteer_api.service.task.VolunteerOppService;
import com.example.MyVolunteer_api.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/taskSignups")
public class TaskSignupsController {

    @Autowired
    private TaskSignupsService taskSignupsService;

    @Autowired
    private VolunteerOppService volunteerOppService;

    @Autowired
    private UserService userService;


    @GetMapping("/getAllForVol")
    public String getAllSignupsForVolunteer(Model model, RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        String email = userDetails.getUsername();

        User user = userService.findByEmail(email);

        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "user not found");
            return "redirect:/test/home";
        }

        List<SignupForVolDto> list = Collections.emptyList();
        if (user.getRole() == Role.VOLUNTEER) {
            list = taskSignupsService.getAllSignupsByVolunteer((Volunteer) user);
        }
        model.addAttribute("signups", list);
        return "task/participationHistory";
    }

    @GetMapping("/getAllTaskForVol")
    public String getAllTaskSignupsByVolunteer(Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        String email = userDetails.getUsername();

        User user = userService.findByEmail(email);

        if (user == null) {
            return "User not found";
        }

        List<VolunteerOpportunitiesDTO> list = Collections.emptyList();
        if (user.getRole() == Role.VOLUNTEER) {
            list = taskSignupsService.getAllTaskSignupsByVolunteer((Volunteer) user);
        }
        model.addAttribute("volunteerOpportunities", list);
        model.addAttribute("user", "Vol");
        return "task/VolunteerOpp";
    }

    @GetMapping("/getAllForTask")
    public ResponseEntity<?> getAllSignupsForTask(@Valid @RequestBody SignupForTaskRequest taskRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        String email = userDetails.getUsername();

        User user = userService.findByEmail(email);

        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }
        VolunteerOpportunities task = volunteerOppService.findById(taskRequest.getTaskId()).orElseThrow();
        if (user.getRole() == Role.ORGANIZATION && task.getCreatedBy() == user) {
            return ResponseEntity.ok(task.getTaskSignups().stream().map(this::taskSignupToDto).collect(Collectors.toList()));
        }
        return ResponseEntity.ok("failed to fetch");
    }

    @PostMapping("/create")
    public String createTaskSignup(@Valid @ModelAttribute SignupForTaskRequest taskRequest, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        String email = userDetails.getUsername();

        User user = userService.findByEmail(email);

        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "user not found");
            return "redirect:/test/home";
        }
        VolunteerOpportunities task = volunteerOppService.findById(taskRequest.getTaskId()).orElseThrow();

        TaskSignups taskSignups = new TaskSignups();
        if (user.getRole() == Role.VOLUNTEER) {
            Optional<TaskSignups> taskSignups1 = taskSignupsService.getSignupByVolunteerAndTask((Volunteer) user, task);
            if (taskSignups1.isPresent()) {
                taskSignups.setSignupId(taskSignups1.get().getSignupId());
            }
            taskSignups.setVolunteer((Volunteer) user);
            taskSignups.setTask(task);
            taskSignups.setName(user.getName());
            taskSignups.setEmail(user.getEmail());
            taskSignups.setStatus(SignUpStatus.TAKEN);
            taskSignups.setTaskTitle(task.getTitle());
            taskSignups.setTaskDesc(task.getDescription());
            taskSignups.setOrganizedBy(task.getOrganization_name());
            taskSignups.setAssignedDate(task.getStartsAt());
            taskSignups.setCompletionDate(task.getEndsAt());

            taskSignups = taskSignupsService.createSignUp(taskSignups);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Successfully Registered");
        return "redirect:/volunteerOpp/details/" + taskRequest.getTaskId();
    }

    @PostMapping("/cancel")
    public String cancelTaskSignup(@Valid @ModelAttribute SignupForTaskRequest taskRequest, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        String email = userDetails.getUsername();

        User user = userService.findByEmail(email);

        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "user not found");
            return "redirect:/test/home";
        }
        VolunteerOpportunities task = volunteerOppService.findById(taskRequest.getTaskId()).orElseThrow();

        if (user.getRole() == Role.VOLUNTEER) {
            taskSignupsService.cancelSignUp((Volunteer) user, task);
            redirectAttributes.addFlashAttribute("successMessage", "Successfully Canceled");
            return "redirect:/volunteerOpp/details/" + taskRequest.getTaskId();
        }
        redirectAttributes.addFlashAttribute("errorMessage", "some error");
        return "redirect:/volunteerOpp/details/" + taskRequest.getTaskId();
    }

    private SignupForTaskDto taskSignupToDto(TaskSignups taskSignups) {
        SignupForTaskDto taskSignup = new SignupForTaskDto();
        taskSignup.setTaskDesc(taskSignups.getTaskDesc());
        taskSignup.setTaskTitle(taskSignups.getTaskTitle());
        taskSignup.setName(taskSignups.getName());
        taskSignup.setEmail(taskSignups.getEmail());
        taskSignup.setStatus(taskSignups.getStatus());
        taskSignup.setAssignedDate(taskSignups.getAssignedDate());
        taskSignup.setCompletionDate(taskSignups.getCompletionDate());
        return taskSignup;

    }

}
