package wireformats;

public class TotalDifference extends Payload{

	//================================================================================
	// Overridden Constructors
	//================================================================================
	public TotalDifference(int number){
		super(number);
		type = Constants.Total_Difference;
	}
	
	public TotalDifference(){
		super();
		type = Constants.Total_Difference;
	}
}
