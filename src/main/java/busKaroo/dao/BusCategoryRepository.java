package busKaroo.dao;

import busKaroo.model.BusCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusCategoryRepository extends JpaRepository<BusCategory, Long> {
    List<BusCategory> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}
