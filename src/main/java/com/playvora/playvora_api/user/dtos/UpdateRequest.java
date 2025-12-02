package com.playvora.playvora_api.user.dtos;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateRequest {
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Optional<String> firstName;
    
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Optional<String> lastName;
    
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Optional<String> nickname;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Optional<String> profilePictureUrl;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Optional<String> country;
}
