package com.example.MyVolunteer_api.controller.task;

import com.example.MyVolunteer_api.constants.Role;
import com.example.MyVolunteer_api.dto.task.RatingRequest;
import com.example.MyVolunteer_api.model.auth.UserPrincipal;
import com.example.MyVolunteer_api.model.task.TaskRatingId;
import com.example.MyVolunteer_api.model.task.TaskRatings;
import com.example.MyVolunteer_api.model.task.TaskSignups;
import com.example.MyVolunteer_api.model.user.Organization;
import com.example.MyVolunteer_api.model.user.User;
import com.example.MyVolunteer_api.model.user.Volunteer;
import com.example.MyVolunteer_api.service.task.TaskRatingsService;
import com.example.MyVolunteer_api.service.task.TaskSignupsService;
import com.example.MyVolunteer_api.service.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/taskRatings")
public class TaskRatingsController {

    @Autowired
    private TaskRatingsService taskRatingsService;

    @Autowired
    private TaskSignupsService taskSignupsService;

    @Autowired
    private UserService userService;


    @PostMapping("/{signUpId}/volunteer")
    public String submitRatingByVolunteer(@PathVariable Integer signUpId, @Valid @ModelAttribute RatingRequest request, RedirectAttributes redirectAttributes) {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
            String email = userDetails.getUsername();

            User user = userService.findByEmail(email);
            if (user == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "User not found");
                return "redirect:/test/home";
            }

            TaskSignups taskSignups = taskSignupsService.findById(signUpId).orElseThrow(() -> new EntityNotFoundException("signup id not found"));

            if (user.getRole() == Role.VOLUNTEER) {
                if (taskSignups.getVolunteer() != user) {
                    throw new AuthorizationDeniedException("user is not associated with this signup");
                }

                if (taskSignups.getTask() == null) {
                    throw new EntityNotFoundException("user can't rate this task");
                }

                TaskRatings taskRating = taskRatingsService.findByTaskAndVolunteer(taskSignups.getTask(), (Volunteer) user).orElse(new TaskRatings());
                TaskRatingId taskRatingId = new TaskRatingId();
                taskRatingId.setTaskId(taskSignups.getTask().getTaskId());
                taskRatingId.setVolunteerId(user.getId());
                taskRatingId.setOrganizationId(taskSignups.getTask().getCreatedBy().getId());

                taskRating.setId(taskRatingId);
                taskRating.setTask(taskSignups.getTask());
                taskRating.setVolunteer((Volunteer) user);
                taskRating.setOrganization(taskSignups.getTask().getCreatedBy());
                taskRating.setRatingByVol(request.getRating());
                taskRating.setFeedbackByVol(request.getFeedback());
                taskRatingsService.updateRatings(taskRating);
                redirectAttributes.addFlashAttribute("successMessage", "Rating submitted successfully.");
                return "redirect:/taskSignups/getAllForVol";
            }
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Entity not found");
            return "redirect:/test/home";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/test/home";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "some error");
        return "redirect:/test/home";

    }

    @PostMapping("/{signUpId}/organization")
    public String submitRatingByOrganization(@PathVariable Integer signUpId, @Valid @ModelAttribute RatingRequest request, RedirectAttributes redirectAttributes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
            String email = userDetails.getUsername();

            User user = userService.findByEmail(email);
            if (user == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "User not found");
                return "redirect:/test/home";
            }

            TaskSignups taskSignups = taskSignupsService.findById(signUpId).orElseThrow(() -> new EntityNotFoundException("signup id not found"));

            if (user.getRole() == Role.ORGANIZATION) {
                if (taskSignups.getTask() == null) {
                    throw new EntityNotFoundException("organization can't rate this volunteer");
                }

                if (taskSignups.getVolunteer() == null) {
                    throw new EntityNotFoundException("organization can't rate this volunteer");
                }

                if (taskSignups.getTask().getCreatedBy() != user) {
                    throw new AuthorizationDeniedException("organization is not associated with this signup");
                }

                TaskRatings taskRating = taskRatingsService.findByTaskAndVolunteer(taskSignups.getTask(), taskSignups.getVolunteer()).orElse(new TaskRatings());
                TaskRatingId taskRatingId = new TaskRatingId();
                taskRatingId.setTaskId(taskSignups.getTask().getTaskId());
                taskRatingId.setVolunteerId(taskSignups.getVolunteer().getId());
                taskRatingId.setOrganizationId(user.getId());

                taskRating.setId(taskRatingId);
                taskRating.setTask(taskSignups.getTask());
                taskRating.setVolunteer(taskSignups.getVolunteer());
                taskRating.setOrganization((Organization) user);
                taskRating.setRatingByOrg(request.getRating());
                taskRating.setFeedbackByOrg(request.getFeedback());
                taskRatingsService.updateRatings(taskRating);
                redirectAttributes.addFlashAttribute("successMessage", "Rating submitted successfully.");
                return "redirect:/volunteerOpp/details/" + taskSignups.getTask().getTaskId();
            }
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Entity not found");
            return "redirect:/test/home";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/test/home";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "some error");
        return "redirect:/test/home";
    }

    @GetMapping("forVol")
    public ResponseEntity<?> getAllRatingsForVol() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        String email = userDetails.getUsername();

        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        if (user.getRole() == Role.VOLUNTEER) {
            return ResponseEntity.ok(taskRatingsService.findByVolunteer((Volunteer) user, "for"));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("forOrg")
    public ResponseEntity<?> getAllRatingsForOrg() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        String email = userDetails.getUsername();

        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }
        if (user.getRole() == Role.ORGANIZATION) {
            return ResponseEntity.ok(taskRatingsService.findByOrganization((Organization) user, "for"));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("ratings")
    public String getRatings(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("top10Vol", taskRatingsService.top10Volunteers());
        model.addAttribute("top10Org", taskRatingsService.top10Organizations());
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return "task/ratingsPage";
        }

        if (authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
            String email = userDetails.getUsername();
            User user = userService.findByEmail(email);

            if (user != null) {
                if (user.getRole() == Role.ORGANIZATION) {
                    model.addAttribute("ratingsTaken", taskRatingsService.findByOrganization((Organization) user, "for"));
                    model.addAttribute("ratingsGiven", taskRatingsService.findByOrganization((Organization) user, "given"));
                } else if (user.getRole() == Role.VOLUNTEER) {
                    model.addAttribute("ratingsTaken", taskRatingsService.findByVolunteer((Volunteer) user, "for"));
                    model.addAttribute("ratingsGiven", taskRatingsService.findByVolunteer((Volunteer) user, "given"));
                }
            }
        }

        return "task/ratingsPage";
    }

}
