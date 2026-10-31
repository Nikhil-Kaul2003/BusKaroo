package busKaroo.service;

import busKaroo.dao.BusRepository;
import busKaroo.dao.BusCategoryRepository;
import busKaroo.dto.request.BusRequest;
import busKaroo.exception.BusCategoryNotFoundException;
import busKaroo.exception.BusNotFoundException;
import busKaroo.model.Bus;
import busKaroo.model.BusCategory;
import busKaroo.model.enums.BusStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BusService {

    private final BusRepository busRepository;
    private final BusCategoryRepository categoryRepository;

    public BusService(BusRepository busRepository, BusCategoryRepository categoryRepository) {
        this.busRepository = busRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Bus> findAll() {
        return busRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Bus findById(Long id) {
        return busRepository.findById(id).orElseThrow(() -> new BusNotFoundException(id));
    }

    @Transactional
    public Bus create(BusRequest request) {
        Bus bus = new Bus();
        applyRequest(bus, request);
        return busRepository.save(bus);
    }

    @Transactional
    public Bus update(Long id, BusRequest request) {
        Bus bus = findById(id);
        applyRequest(bus, request);
        return busRepository.save(bus);
    }

    @Transactional
    public void updateStatus(Long id, BusStatus status) {
        Bus bus = findById(id);
        bus.setStatus(status);
        busRepository.save(bus);
    }

    @Transactional
    public void delete(Long id) {
        if (!busRepository.existsById(id)) {
            throw new BusNotFoundException(id);
        }
        busRepository.deleteById(id);
    }

    private void applyRequest(Bus bus, BusRequest request) {
        BusCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusCategoryNotFoundException(request.categoryId()));
        bus.setRegistrationNumber(request.registrationNumber().trim().toUpperCase());
        bus.setModel(request.model().trim());
        bus.setManufacturer(request.manufacturer().trim());
        bus.setYear((short) request.year());
        bus.setTotalSeats(request.totalSeats());
        bus.setSeatLayout(request.seatLayout().trim());
        bus.setStatus(request.status());
        bus.setCategory(category);
    }
}
