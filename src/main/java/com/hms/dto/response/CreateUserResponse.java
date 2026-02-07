package com.hms.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateUserResponse extends UserResponse {

    private String generatedPassword;

    public CreateUserResponse() {
        super();
    }
}
