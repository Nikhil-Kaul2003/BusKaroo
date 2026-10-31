package busKaroo.service;

import busKaroo.dao.BusCategoryRepository;
import busKaroo.dao.BusRepository;
import busKaroo.dto.request.BusRequest;
import busKaroo.exception.BusCategoryNotFoundException;
import busKaroo.exception.BusNotFoundException;
import busKaroo.model.Bus;
import busKaroo.model.BusCategory;
import busKaroo.model.enums.BusStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusServiceTest {

    @Mock
    private BusRepository busRepository;

    @Mock
    private BusCategoryRepository categoryRepository;

    @InjectMocks
    private BusService service;

    @Test
    void createNormalizesRegistrationAndAssignsCategory() {
        BusCategory category = new BusCategory();
        category.setName("Luxury AC Coach");
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(busRepository.save(any(Bus.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Bus created = service.create(request(3L));

        assertEquals("MH-01-AB-1234", created.getRegistrationNumber());
        assertEquals("Luxury AC Coach", created.getCategory().getName());
        assertEquals(40, created.getTotalSeats());
        verify(busRepository).save(created);
    }

    @Test
    void createWithUnknownCategoryThrowsDomainException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusCategoryNotFoundException.class, () -> service.create(request(99L)));
        verify(busRepository, never()).save(any());
    }

    @Test
    void updateMissingBusThrowsDomainException() {
        when(busRepository.findById(81L)).thenReturn(Optional.empty());

        assertThrows(BusNotFoundException.class, () -> service.update(81L, request(3L)));
    }

    @Test
    void deleteMissingBusThrowsDomainException() {
        when(busRepository.existsById(81L)).thenReturn(false);

        assertThrows(BusNotFoundException.class, () -> service.delete(81L));
        verify(busRepository, never()).deleteById(any());
    }

    private BusRequest request(Long categoryId) {
        return new BusRequest(
                " mh-01-ab-1234 ",
                "9600 B8R",
                "Volvo",
                2024,
                40,
                "{\"rows\":10,\"seatsPerRow\":4,\"categories\":{}}",
                BusStatus.ACTIVE,
                categoryId);
    }
}
