package busKaroo.service;

import busKaroo.dao.BusCategoryRepository;
import busKaroo.dto.request.BusCategoryRequest;
import busKaroo.exception.BusCategoryNotFoundException;
import busKaroo.model.BusCategory;
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
class BusCategoryServiceTest {

    @Mock
    private BusCategoryRepository repository;

    @InjectMocks
    private BusCategoryService service;

    @Test
    void createTrimsCategoryFields() {
        when(repository.save(any(BusCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BusCategory created = service.create(new BusCategoryRequest(" AC Sleeper ", " Overnight coach "));

        assertEquals("AC Sleeper", created.getName());
        assertEquals("Overnight coach", created.getDescription());
        verify(repository).save(created);
    }

    @Test
    void updateMissingCategoryThrowsDomainException() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(BusCategoryNotFoundException.class,
                () -> service.update(42L, new BusCategoryRequest("Luxury", null)));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteExistingCategory() {
        BusCategory category = new BusCategory();
        when(repository.findById(7L)).thenReturn(Optional.of(category));

        service.delete(7L);

        verify(repository).delete(category);
    }
}
