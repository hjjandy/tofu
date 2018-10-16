package edu.purdue.cs.toydroid.soot.util.toDex.instructions;

import edu.purdue.cs.toydroid.soot.util.toDex.Register;

/**
 * Interface for instructions that need two registers.
 */
public interface TwoRegInsn extends OneRegInsn {
	
	static final int REG_B_IDX = REG_A_IDX + 1;
	
	Register getRegB();
}