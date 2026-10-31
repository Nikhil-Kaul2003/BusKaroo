package busKaroo.service;

import busKaroo.dao.BusCategoryRepository;
import busKaroo.dto.request.BusCategoryRequest;
import busKaroo.exception.BusCategoryNotFoundException;
import busKaroo.model.BusCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BusCategoryService {
    private final BusCategoryRepository repository;

    public BusCategoryService(BusCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BusCategory> findAll() {
        return repository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public BusCategory findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusCategoryNotFoundException(id));
    }

    @Transactional
    public BusCategory create(BusCategoryRequest request) {
        BusCategory category = new BusCategory();
        category.setName(request.name().trim());
        category.setDescription(cleanDescription(request.description()));
        return repository.save(category);
    }

    @Transactional
    public BusCategory update(Long id, BusCategoryRequest request) {
        BusCategory category = findById(id);
        category.setName(request.name().trim());
        category.setDescription(cleanDescription(request.description()));
        return repository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
    }

    private String cleanDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
