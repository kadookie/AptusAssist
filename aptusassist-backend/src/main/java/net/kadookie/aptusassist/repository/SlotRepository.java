package net.kadookie.aptusassist.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import net.kadookie.aptusassist.entity.Slot;

import java.time.LocalDate;
import java.util.List;

/**
 * JPA repository for {@link Slot} entity persistence operations.
 * <p>
 * Features:
 * <ul>
 * <li>Inherits all standard CRUD operations from JpaRepository</li>
 * <li>Custom query methods following Spring Data naming conventions</li>
 * <li>Transactional by default (read-only for queries)</li>
 * <li>Automatic query derivation from method names</li>
 * </ul>
 * <p>
 * Performance Notes:
 * <ul>
 * <li>Queries are optimized by Hibernate</li>
 * <li>Results are not cached by default</li>
 * <li>Batch operations available via JpaRepository</li>
 * </ul>
 * <p>
 * Query Methods:
 * <ul>
 * <li>findByDate - Retrieves all slots for a specific date</li>
 * </ul>
 */
public interface SlotRepository extends JpaRepository<Slot, Long> {
    /**
     * Finds all time slots for a specific date.
     * <p>
     * Query execution:
     * <ul>
     * <li>Generates SQL: SELECT * FROM slot WHERE date = ?</li>
     * <li>Returns empty list if no slots found</li>
     * <li>Results ordered by database default (typically by primary key)</li>
     * </ul>
     *
     * @param date Date to search slots for (yyyy-MM-dd format)
     * @return List of Slot entities matching the date (never null)
     * @see Slot
     */
    List<Slot> findByDate(LocalDate date);
}
