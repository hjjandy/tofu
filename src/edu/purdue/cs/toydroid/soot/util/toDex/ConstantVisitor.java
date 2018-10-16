package edu.purdue.cs.toydroid.soot.util.toDex;

import edu.purdue.cs.toydroid.soot.util.toDex.instructions.*;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.writer.builder.BuilderReference;
import org.jf.dexlib2.writer.builder.DexBuilder;
import soot.jimple.*;
import soot.util.Switchable;

/**
 * A visitor that builds a list of instructions from the Jimple constants it visits.<br>
 * <br>
 * Use {@link Switchable#apply(soot.util.Switch)} with this visitor to add statements.
 * These are added to the instructions in the {@link StmtVisitor}.<br>
 * Do not forget to use {@link #setDestination(Register)} to set the storage location for the constant.
 * 
 * @see StmtVisitor
 */
class ConstantVisitor extends AbstractConstantSwitch {
	
	private final DexBuilder dexFile;
	private StmtVisitor stmtV;
	
	private Register destinationReg;
	
    private Stmt origStmt;

	public ConstantVisitor(DexBuilder dexFile, StmtVisitor stmtV) {
		this.stmtV = stmtV;
		this.dexFile = dexFile;
	}
	
	public void setDestination(Register destinationReg) {
		this.destinationReg = destinationReg;
	}

    public void setOrigStmt(Stmt stmt) {
        this.origStmt = stmt;
    }

	public void defaultCase(Object o) {
		// const* opcodes not used since there seems to be no point in doing so:
		// CONST_HIGH16, CONST_WIDE_HIGH16
		throw new Error("unknown Object (" + o.getClass() + ") as Constant: " + o);
	}
	
	public void caseStringConstant(StringConstant s) {
		BuilderReference ref = dexFile.internStringReference(s.value);
        stmtV.addInsn(new Insn21c(Opcode.CONST_STRING, destinationReg, ref), origStmt);
	}
	
	public void caseClassConstant(ClassConstant c) {
		// "array class" types are unmodified
		boolean classIsArray = c.value.startsWith("[");
		String className = classIsArray ? c.value : SootToDexUtils.getDexClassName(c.value);
		BuilderReference referencedClass = dexFile.internTypeReference(className);
        stmtV.addInsn(new Insn21c(Opcode.CONST_CLASS, destinationReg, referencedClass), origStmt);
	}
	
	public void caseLongConstant(LongConstant l) {
		long constant = l.value;
        stmtV.addInsn(buildConstWideInsn(constant), origStmt);
	}
	
	private Insn buildConstWideInsn(long literal) {
		if (SootToDexUtils.fitsSigned16(literal)) {
			return new Insn21s(Opcode.CONST_WIDE_16, destinationReg, (short) literal);
		} else if (SootToDexUtils.fitsSigned32(literal)) {
			return new Insn31i(Opcode.CONST_WIDE_32, destinationReg, (int) literal);
		} else {
			return new Insn51l(Opcode.CONST_WIDE, destinationReg, literal);
		}
	}
	
	public void caseDoubleConstant(DoubleConstant d) {
		long longBits = Double.doubleToLongBits(d.value);
        stmtV.addInsn(buildConstWideInsn(longBits), origStmt);
	}
	
	public void caseFloatConstant(FloatConstant f) {
		int intBits = Float.floatToIntBits(f.value);
        stmtV.addInsn(buildConstInsn(intBits), origStmt);
	}
	
	private Insn buildConstInsn(int literal) {
		if (SootToDexUtils.fitsSigned4(literal))
			return new Insn11n(Opcode.CONST_4, destinationReg, (byte) literal);
		else if (SootToDexUtils.fitsSigned16(literal))
			return new Insn21s(Opcode.CONST_16, destinationReg, (short) literal);
		else
			return new Insn31i(Opcode.CONST, destinationReg, literal);
	}

	public void caseIntConstant(IntConstant i) {
        stmtV.addInsn(buildConstInsn(i.value), origStmt);
	}
	
	public void caseNullConstant(NullConstant v) {
		// dex bytecode spec says: "In terms of bitwise representation, (Object) null == (int) 0."
        stmtV.addInsn(buildConstInsn(0), origStmt);
	}

}
