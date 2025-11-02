package com.demo.community.common.dto;

import com.demo.community.common.domain.entity.SessionErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class ApiResponse<T> {

    private String message;

    private SessionErrorCode errorCode;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private T data;


    public ApiResponse(String message, T data){
        this.message = message;
        this.data = data;
        this.errorCode = null;
    }
}
