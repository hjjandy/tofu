package edu.purdue.cs.toydroid.soot.util.toDex.instructions;

import edu.purdue.cs.toydroid.soot.util.toDex.LabelAssigner;
import edu.purdue.cs.toydroid.soot.util.toDex.Register;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.instruction.BuilderInstruction22b;

import java.util.BitSet;

/**
 * The "22b" instruction format: It needs two 16-bit code units, has two registers
 * and is used for a 8-bit literal (hence the "b" for "byte").<br>
 * <br>
 * It is used by the "/lit8" opcodes for binary operations.
 */
public class Insn22b extends AbstractInsn implements TwoRegInsn {
	
	private byte litC;

	public Insn22b(Opcode opc, Register regA, Register regB, byte litC) {
		super(opc);
		regs.add(regA);
		regs.add(regB);
		this.litC = litC;
	}
	
	public Register getRegA() {
		return regs.get(REG_A_IDX);
	}
	
	public Register getRegB() {
		return regs.get(REG_B_IDX);
	}
	
	public byte getLitC() {
		return litC;
	}

	@Override
	protected BuilderInstruction getRealInsn0(LabelAssigner assigner) {
		return new BuilderInstruction22b(opc, (short) getRegA().getNumber(),
				(short) getRegB().getNumber(), getLitC());
	}
	
	@Override
	public BitSet getIncompatibleRegs() {
		BitSet incompatRegs = new BitSet(2);
		if (!getRegA().fitsShort()) {
			incompatRegs.set(REG_A_IDX);
		}
		if (!getRegB().fitsShort()) {
			incompatRegs.set(REG_B_IDX);
		}
		return incompatRegs;
	}
	
	@Override
	public String toString() {
		return super.toString() + " lit: " + getLitC();
	}
}