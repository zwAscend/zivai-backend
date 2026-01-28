package zw.co.zivai.core_backend.dtos;

import lombok.Data;

@Data
public class CreateSchoolRequest {
    private String code;
    private String name;
    private String countryCode;
}
