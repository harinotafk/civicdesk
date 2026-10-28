package com.ram.civicdesk.service;

import com.ram.civicdesk.dto.CategoryResponse;
import com.ram.civicdesk.dto.WardResponse;
import com.ram.civicdesk.dto.ZoneResponse;
import com.ram.civicdesk.exception.ResourceNotFoundException;
import com.ram.civicdesk.repository.CategoryRepository;
import com.ram.civicdesk.repository.WardRepository;
import com.ram.civicdesk.repository.ZoneRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Zones, wards and categories: the lookup data the complaint form needs. */
@Service
public class ReferenceService {

    private final ZoneRepository zoneRepository;
    private final WardRepository wardRepository;
    private final CategoryRepository categoryRepository;

    public ReferenceService(ZoneRepository zoneRepository, WardRepository wardRepository,
                            CategoryRepository categoryRepository) {
        this.zoneRepository = zoneRepository;
        this.wardRepository = wardRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> zones() {
        return zoneRepository.findAll(Sort.by("id")).stream()
                .map(z -> new ZoneResponse(z.getId(), z.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WardResponse> wardsInZone(Integer zoneId) {
        if (!zoneRepository.existsById(zoneId)) {
            throw new ResourceNotFoundException("Zone " + zoneId + " not found");
        }
        return wardRepository.findByZoneIdOrderByWardNumber(zoneId).stream()
                .map(Mappers::ward)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> categories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(Mappers::category)
                .toList();
    }
}
