package wireformats;

public class Verification extends Payload{

	//================================================================================
	// Overridden Constructors
	//================================================================================
	public Verification(int number){
		super(number);
		type = Constants.Verification;
	}
	
	public Verification(){
		super();
		type = Constants.Verification;
	}
}
