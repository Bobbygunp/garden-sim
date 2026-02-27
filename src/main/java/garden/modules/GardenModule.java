package garden.modules;

import garden.core.Garden;

/**
 * Interface for all standalone garden modules.
 * Modules operate autonomously, monitoring sensors and acting on the garden.
 */
public interface GardenModule {

    /** Human-readable name of this module. */
    String getModuleName();

    /** Called each simulation tick. */
    void update(Garden garden);

    /** Enable or disable this module. */
    void setEnabled(boolean enabled);

    /** Whether this module is currently active. */
    boolean isEnabled();

    /** Get a status summary for display. */
    String getStatusSummary();
}
