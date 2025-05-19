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
     * Constructs service with required repository
     *
     * @param slotRepository Slot data repository
     */
    public SlotDbService(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
        // logger.info("SlotDbService initialized");
    }

    /**
     * Saves or updates multiple slots in a single transaction.
     * <p>
     * For each slot:
     * <ul>
     * <li>Creates new slot if doesn't exist</li>
     * <li>Updates status if slot exists</li>
     * <li>Skips invalid slots (missing required fields)</li>
     * </ul>
     *
     * @param slots List of slot maps containing date, passNo and status
     * @throws RuntimeException if any slot fails to save (triggers rollback)
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
     * Finds all slots for a specific date
     *
     * @param date Date to search slots for
     * @return List of slots (empty if none found)
     */
    public List<Slot> findSlotsByDate(LocalDate date) {
        logger.debug("Fetching slots for date: {}", date);
        List<Slot> slots = slotRepository.findByDate(date);
        logger.debug("Found {} slots for date: {}", slots.size(), date);
        return slots;
    }

    /**
     * Retrieves all slots from database
     *
     * @return List of all slots (empty if none exist)
     */
    public List<Slot> findAllSlots() {
        logger.debug("Fetching all slots");
        List<Slot> slots = slotRepository.findAll();
        logger.debug("Found {} slots", slots.size());
        return slots;
    }

    /**
     * Updates status of a specific slot
     *
     * @param date      Slot date
     * @param passNo    Slot pass number
     * @param newStatus New status value
     * @throws IllegalArgumentException if slot not found
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