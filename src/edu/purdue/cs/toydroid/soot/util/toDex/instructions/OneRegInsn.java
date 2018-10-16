package edu.purdue.cs.toydroid.soot.util.toDex.instructions;

import edu.purdue.cs.toydroid.soot.util.toDex.Register;

/**
 * Interface for instructions that need one register.
 */
public interface OneRegInsn extends Insn {
	
	static final int REG_A_IDX = 0;

	Register getRegA();
}