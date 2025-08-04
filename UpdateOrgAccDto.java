package com.example.MyVolunteer_api.dto.auth;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrgAccDto {


    @Column(nullable = false)
    @NotBlank(message = "name may not be blank")
    private String name;

    @Column(nullable = false)
    @NotBlank(message = "phone may not be blank")
    private String phone;

    @Column(nullable = false)
    @NotBlank(message = "gstNumber may not be blank")
    private String gstNumber;

    @Column(nullable = false)
    @NotBlank(message = "location may not be blank")
    private String location;


}
