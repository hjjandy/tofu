package edu.purdue.cs.toydroid.soot.tofu.type;


public class TypeSystem {
	public static final Integer Nil = makeType(0);
	public static final Integer Removable = makeType(1);
	public static final Integer UnRemovable = makeType(2);
	public static final Integer UIRelated = makeType(3);
	public static final Integer UIRelated_Click = makeType(4);
	public static final Integer UIRelated_Banner = makeType(5);
	public static final Integer UIData = makeType(6);
	private static int IdxInputUIData = UIData.intValue() + 1;

	private static Integer makeType(int i) {
		return new Integer(i);
	}

	public static Integer newInputUIDataType() {
		Integer i = new Integer(IdxInputUIData++);
		return i;
	}

	public static boolean isNil(Integer i) {
		return Nil.equals(i);
	}

	public static boolean isRemovable(Integer i) {
		return Removable.equals(i);
	}

	public static boolean isUnremovable(Integer i) {
		return UnRemovable.equals(i);
	}

	public static boolean isUIRelated(Integer i) {
		if (i != null) {
			return UIRelated.intValue() <= i.intValue() && i.intValue() <= UIRelated_Banner.intValue();
		}
		return false;
	}

	public static boolean isUIData(Integer i) {
		return UIData.equals(i);
	}

	public static boolean isInputUIData(Integer i) {
		if (i != null) {
			return UIData.intValue() < i.intValue() && i.intValue() < IdxInputUIData;
		}
		return false;
	}
}
