package net.kadookie.aptusassist.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import net.kadookie.aptusassist.entity.Slot;

import java.time.LocalDate;
import java.util.List;

/**
 * JPA repository for {@link Booking} entity persistence operations.
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
 */
public interface SlotRepository extends JpaRepository<Slot, Long> {
    /**
     * Finds all bookings for a specific date
     *
     * @param date Date to search bookings for
     * @return List of bookings matching the date (empty if none found)
     */
    List<Slot> findByDate(LocalDate date);
}
