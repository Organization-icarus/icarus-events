package com.icarus.events;

/**
 * Represents a single field of an Event, with a name and value.
 * Used by EventDetailsAdapter to display each field in a ListView row.
 *
 * @author Bradley Bravender
 */
public class EventField {
    private String name;
    private String value;

    public EventField(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public String getValue() { return value; }
}
