package il.ac.technion.cs.sd.msg;

public class AuxDerived extends AuxBase {
	public String bfield;
	
	@Override
	String id() {
		return "DERIVED";
	}
}
