package com.ecommerce.backend.dto;

import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
public class SignUpRequest {
    private String username;
    private String password;
}
