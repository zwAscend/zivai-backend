package zw.co.zivai.core_backend.common.services.school;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.dtos.schools.CreateSchoolRequest;
import zw.co.zivai.core_backend.common.exceptions.NotFoundException;
import zw.co.zivai.core_backend.common.models.lms.school.School;
import zw.co.zivai.core_backend.common.repositories.school.SchoolRepository;

@Service
@RequiredArgsConstructor
public class SchoolService {
    private final SchoolRepository schoolRepository;

    public School create(CreateSchoolRequest request) {
        School school = new School();
        school.setCode(request.getCode());
        school.setName(request.getName());
        if (request.getCountryCode() != null) {
            school.setCountryCode(request.getCountryCode());
        }
        return schoolRepository.save(school);
    }

    public List<School> list() {
        return schoolRepository.findAllByDeletedAtIsNull();
    }

    public School get(UUID id) {
        return schoolRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("School not found: " + id));
    }
}
