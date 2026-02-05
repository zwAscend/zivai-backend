package zw.co.zivai.core_backend.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.AdminCreateEdgeNodeRequest;
import zw.co.zivai.core_backend.dtos.AdminEdgeNodeDto;
import zw.co.zivai.core_backend.dtos.AdminSummaryDto;
import zw.co.zivai.core_backend.dtos.AdminUpdateEdgeNodeRequest;
import zw.co.zivai.core_backend.services.AdminService;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/summary")
    public AdminSummaryDto summary() {
        return adminService.getSummary();
    }

    @GetMapping("/edge-nodes")
    public List<AdminEdgeNodeDto> edgeNodes() {
        return adminService.getEdgeNodes();
    }

    @PostMapping("/edge-nodes")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminEdgeNodeDto registerEdgeNode(@RequestBody AdminCreateEdgeNodeRequest request) {
        return adminService.registerEdgeNode(request);
    }

    @PutMapping("/edge-nodes/{id}")
    public AdminEdgeNodeDto updateEdgeNode(@PathVariable java.util.UUID id, @RequestBody AdminUpdateEdgeNodeRequest request) {
        return adminService.updateEdgeNode(id, request);
    }

    @DeleteMapping("/edge-nodes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEdgeNode(@PathVariable java.util.UUID id) {
        adminService.deleteEdgeNode(id);
    }
}
