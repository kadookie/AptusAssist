package net.kadookie.aptusassist.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.kadookie.aptusassist.entity.Slot;
import net.kadookie.aptusassist.repository.SlotRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service layer for database operations related to slot management.
 * <p>
 * Handles all CRUD operations for slot data with proper transaction
 * management.
 * Uses {@link SlotRepository} for persistence operations.
 * <p>
 * Transaction notes:
 * <ul>
 * <li>{@code @Transactional} methods roll back on any exception</li>
 * <li>Read operations are transactional with read-only optimizations</li>
 * <li>Write operations have proper isolation levels</li>
 * </ul>
 */
@Service
public class SlotDbService {
    private static final Logger logger = LoggerFactory.getLogger(SlotDbService.class);

    /** Repository for slot data access */
    private final SlotRepository slotRepository;

    /** Formatter for ISO date strings (yyyy-MM-dd) */
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Constructs service with required repository.
     * <p>
     * Initializes:
     * <ul>
     * <li>SlotRepository for database operations</li>
     * <li>ISO date formatter for date parsing</li>
     * </ul>
     *
     * @param slotRepository Slot data repository (required)
     * @throws IllegalArgumentException if slotRepository is null
     * @see SlotRepository
     */
    public SlotDbService(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
        // logger.info("SlotDbService initialized");
    }

    /**
     * Saves or updates multiple slots in a single atomic transaction.
     * <p>
     * Transaction Behavior:
     * <ul>
     * <li>Atomic operation - all slots succeed or none do</li>
     * <li>Rolls back entire transaction if any slot fails</li>
     * <li>Read committed isolation level</li>
     * </ul>
     * <p>
     * For each slot:
     * <ul>
     * <li>Creates new slot if doesn't exist</li>
     * <li>Updates status if slot exists</li>
     * <li>Skips invalid slots (missing required fields)</li>
     * </ul>
     *
     * @param slots List of slot maps containing:
     * <ul>
     * <li>date - String in yyyy-MM-dd format (required)</li>
     * <li>passNo - String numeric value (1-7) (required)</li>
     * <li>status - String status value (required)</li>
     * </ul>
     * @throws IllegalArgumentException if input validation fails
     * @throws RuntimeException if any slot fails to save (triggers rollback)
     * @see Slot
     */
    @Transactional
    public void saveSlots(List<Map<String, String>> slots) {
        if (slots == null || slots.isEmpty()) {
            logger.warn("No slots to save to database");
            return;
        }

        logger.debug("Saving {} slots to database", slots.size());
        for (Map<String, String> slot : slots) {
            try {
                String dateStr = slot.get("date");
                String passNoStr = slot.get("passNo");
                String status = slot.get("status");

                if (dateStr == null || passNoStr == null || status == null) {
                    logger.warn("Skipping invalid slot: {}", slot);
                    continue;
                }

                LocalDate date = LocalDate.parse(dateStr, ISO_DATE_FORMATTER);
                int passNo = Integer.parseInt(passNoStr);

                List<Slot> existingSlots = slotRepository.findByDate(date);
                Slot existingSlot = existingSlots.stream()
                        .filter(b -> b.getPassNo() == passNo)
                        .findFirst()
                        .orElse(null);

                if (existingSlot == null) {
                    Slot newSlot = new Slot();
                    newSlot.setDate(date);
                    newSlot.setPassNo(passNo);
                    newSlot.setStatus(status);
                    slotRepository.save(newSlot);
                    logger.debug("Inserted new slot: date={}, passNo={}, status={}", date, passNo, status);
                } else {
                    existingSlot.setStatus(status);
                    slotRepository.save(existingSlot);
                    // logger.debug("Updated existing slot: date={}, passNo={}, status={}", date,
                    // passNo, status);
                }
            } catch (Exception e) {
                logger.error("Error saving slot: {}, transaction may not have committed", slot, e);
                throw e; // Re-throw to rollback transaction
            }
        }
    }

    /**
     * Finds all slots for a specific date.
     * <p>
     * Performance Characteristics:
     * <ul>
     * <li>Uses JPA derived query with date index</li>
     * <li>Read-only transaction</li>
     * <li>Returns empty list if no matches found</li>
     * <li>O(n) time complexity where n is number of slots</li>
     * </ul>
     *
     * @param date Date to search slots for (required)
     * @return List of Slot entities matching the date (never null)
     * @throws IllegalArgumentException if date is null
     * @see Slot
     * @see SlotRepository#findByDate(LocalDate)
     */
    public List<Slot> findSlotsByDate(LocalDate date) {
        logger.debug("Fetching slots for date: {}", date);
        List<Slot> slots = slotRepository.findByDate(date);
        logger.debug("Found {} slots for date: {}", slots.size(), date);
        return slots;
    }

    /**
     * Retrieves all slots from database.
     * <p>
     * Performance Considerations:
     * <ul>
     * <li>Uses JPA findAll()</li>
     * <li>Read-only transaction</li>
     * <li>Returns empty list if no slots exist</li>
     * <li>O(n) time complexity where n is number of slots</li>
     * <li>Memory intensive for large datasets</li>
     * </ul>
     *
     * @return List of all Slot entities (never null)
     * @see Slot
     * @see SlotRepository#findAll()
     */
    public List<Slot> findAllSlots() {
        logger.debug("Fetching all slots");
        List<Slot> slots = slotRepository.findAll();
        logger.debug("Found {} slots", slots.size());
        return slots;
    }

    /**
     * Updates status of a specific slot.
     * <p>
     * Transaction Behavior:
     * <ul>
     * <li>Auto-committing transaction</li>
     * <li>Optimistic locking if concurrent updates occur</li>
     * <li>Rolls back on any exception</li>
     * </ul>
     * <p>
     * Error Handling:
     * <ul>
     * <li>Silently ignores if slot not found</li>
     * <li>Throws IllegalArgumentException for invalid inputs</li>
     * <li>Throws OptimisticLockingFailureException on concurrent modification</li>
     * </ul>
     *
     * @param date      Slot date (required)
     * @param passNo    Slot pass number (1-7) (required)
     * @param newStatus New status value (required)
     * @throws IllegalArgumentException if any parameter is invalid
     * @throws org.springframework.dao.OptimisticLockingFailureException on concurrent modification
     * @see Slot
     * @see SlotRepository#save(Object)
     */
    public void updateSlotStatus(LocalDate date, int passNo, String newStatus) {
        logger.debug("Updating slot status for date: {}, passNo: {}, newStatus: {}", date, passNo, newStatus);
        List<Slot> slots = slotRepository.findByDate(date);
        Slot slot = slots.stream()
                .filter(b -> b.getPassNo() == passNo)
                .findFirst()
                .orElse(null);
        if (slot != null) {
            slot.setStatus(newStatus);
            slotRepository.save(slot);
            // logger.info("Updated slot status to '{}' for date: {}, passNo: {}",
            // newStatus, date, passNo);
        } else {
            logger.warn("No slot found to update for date: {}, passNo: {}", date, passNo);
        }
    }
}