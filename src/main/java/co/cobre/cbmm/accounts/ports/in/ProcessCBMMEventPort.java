package co.cobre.cbmm.accounts.ports.in;

import co.cobre.cbmm.accounts.application.dto.CBMMEventDTO;

/**
 * Driving port for processing CBMM events
 */
public interface ProcessCBMMEventPort {

    /**
     * Process a CBMM event (Cross Border Money Movement)
     * Creates a debit transaction for origin and credit transaction for destination
     *
     * @param event the CBMM event
     */
    void processCBMMEvent(CBMMEventDTO event);
}

