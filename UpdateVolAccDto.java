package com.example.MyVolunteer_api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVolAccDto {


    @NotBlank(message = "name may not be blank")
    private String name;

    private String phone;

    private List<String> skills;

}
