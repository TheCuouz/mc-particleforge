package com.cristian.particleforge.engine;

/**
 * How {@link BudgetManager} reacts when admitting a new effect would exceed
 * the per-player or global cap.
 *
 * FIFO         — reject the newcomer (queue is first-in first-out).
 * NEWEST_FIRST — evict the oldest existing handle to make room for the new one.
 */
public enum BudgetPolicy { FIFO, NEWEST_FIRST }
