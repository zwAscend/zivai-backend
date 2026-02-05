package zw.co.zivai.core_backend.models.lms;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import zw.co.zivai.core_backend.models.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "addresses", schema = "lms")
public class Address extends BaseEntity {
    @Column(nullable = false)
    private String line1;

    private String line2;
    private String suburb;
    private String city;
    private String district;
    private String province;

    @Column(name = "country_code")
    private String countryCode = "ZW";

    @Column(name = "postal_code")
    private String postalCode;

    private BigDecimal latitude;
    private BigDecimal longitude;
}
