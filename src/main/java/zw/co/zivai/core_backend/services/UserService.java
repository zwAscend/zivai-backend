package zw.co.zivai.core_backend.services;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateUserRequest;
import zw.co.zivai.core_backend.dtos.PhoneNumber;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.models.lookups.Role;
import zw.co.zivai.core_backend.repositories.RoleRepository;
import zw.co.zivai.core_backend.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public User create(CreateUserRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }

        User user = new User();
        user.setExternalId(request.getExternalId());
        user.setEmail(request.getEmail());
        String phoneNumber = PhoneNumber.normalize(request.getPhoneNumber());
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new BadRequestException("Phone number is required");
        }
        user.setPhoneNumber(phoneNumber);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setActive(request.isActive());

        if (request.getRoleCodes() != null && !request.getRoleCodes().isEmpty()) {
            Set<Role> roles = request.getRoleCodes().stream()
                .map(code -> roleRepository.findByCode(code)
                    .orElseThrow(() -> new BadRequestException("Role not found: " + code)))
                .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        return userRepository.save(user);
    }

    public List<User> list() {
        return userRepository.findAll();
    }

    public User get(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }
}
