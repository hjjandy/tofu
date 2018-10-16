package edu.purdue.cs.toydroid.soot.util.toDex.instructions;

import edu.purdue.cs.toydroid.soot.util.toDex.LabelAssigner;
import edu.purdue.cs.toydroid.soot.util.toDex.Register;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;

import java.util.BitSet;
import java.util.List;

/**
 * Interface for the dalvik instruction formats.
 */
public interface Insn extends Cloneable {
	
	Opcode getOpcode();
	
	List<Register> getRegs();
	
	BitSet getIncompatibleRegs();

	boolean hasIncompatibleRegs();
	
	int getMinimumRegsNeeded();
	
	BuilderInstruction getRealInsn(LabelAssigner assigner);

	int getSize();
}