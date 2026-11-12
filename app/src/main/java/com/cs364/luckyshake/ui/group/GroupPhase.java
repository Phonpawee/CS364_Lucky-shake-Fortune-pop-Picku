package com.cs364.luckyshake.ui.group;

/**
 * Phase lifecycle of a group decision session.
 * SETUP: Configuring participant count (2-8) and proposal method.
 * COLLECT: Taking turns to collect secret proposals.
 * FINAL: Tallying votes and performing the winner draw.
 * DONE: Displaying the winning choice and logging to history.
 */
public enum GroupPhase {
    SETUP,
    COLLECT,
    FINAL,
    DONE
}
