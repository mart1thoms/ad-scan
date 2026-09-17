package fr.reniti.adscan.event;

import static fr.reniti.adscan.database.jooq.Tables.EVENT;

import fr.reniti.adscan.database.jooq.tables.records.EventRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class EventRepository {

    private final DSLContext dsl;

    public EventRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<Event> findAll() {
        return dsl.selectFrom(EVENT)
                .orderBy(EVENT.EVENT_DATE.desc())
                .fetch(this::toEvent);
    }

    public Optional<Event> findById(Integer id) {
        return dsl.selectFrom(EVENT)
                .where(EVENT.ID.eq(id))
                .fetchOptional(this::toEvent);
    }

    public Event insert(String name, LocalDate eventDate) {
        EventRecord record = dsl.insertInto(EVENT)
                .set(EVENT.NAME, name)
                .set(EVENT.EVENT_DATE, eventDate)
                .returning()
                .fetchOne();
        return toEvent(record);
    }

    private Event toEvent(EventRecord record) {
        return new Event(record.getId(), record.getName(), record.getEventDate());
    }
}
