package edu.purdue.cs.toydroid.soot.util.toDex.instructions;

import edu.purdue.cs.toydroid.soot.util.toDex.LabelAssigner;
import edu.purdue.cs.toydroid.soot.util.toDex.Register;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21t;

import java.util.BitSet;

/**
 * The "21t" instruction format: It needs two 16-bit code units, has one register
 * and is used for jump targets (hence the "t").<br>
 * <br>
 * It is used e.g. by the opcode "if-eqz" for conditional jumps to a 16-bit wide offset.
 */
public class Insn21t extends InsnWithOffset implements OneRegInsn {
	
	public Insn21t(Opcode opc, Register regA) {
		super(opc);
		regs.add(regA);
	}
	
	public Register getRegA() {
		return regs.get(REG_A_IDX);
	}

	@Override
	protected BuilderInstruction getRealInsn0(LabelAssigner assigner) {
		return new BuilderInstruction21t(opc, (short) getRegA().getNumber(),
				assigner.getOrCreateLabel(target));
	}
	
	@Override
	public BitSet getIncompatibleRegs() {
		BitSet incompatRegs = new BitSet(1);
		if (!getRegA().fitsShort()) {
			incompatRegs.set(REG_A_IDX);
		}
		return incompatRegs;
	}

	@Override
	public int getMaxJumpOffset() {
		return Short.MAX_VALUE;
	}
	
}