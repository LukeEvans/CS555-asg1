package wireformats;

public class SumDifference extends Payload {

	//================================================================================
	// Overridden Constructors
	//================================================================================
	public SumDifference(int number){
		super(number);
		type = Constants.Sum_Difference;
	}
	
	public SumDifference(){
		super();
		type = Constants.Sum_Difference;
	}
}
