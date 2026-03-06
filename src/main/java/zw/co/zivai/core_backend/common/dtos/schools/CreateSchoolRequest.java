package zw.co.zivai.core_backend.common.dtos.schools;

import lombok.Data;

@Data
public class CreateSchoolRequest {
    private String code;
    private String name;
    private String countryCode;
}
