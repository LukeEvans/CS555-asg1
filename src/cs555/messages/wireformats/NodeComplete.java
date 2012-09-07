package cs555.messages.wireformats;

public class NodeComplete extends Payload{

	//================================================================================
	// Overridden Constructors
	//================================================================================
	public NodeComplete(int number){
		super(number);
		type = Constants.Node_Complete;
	}
	
	public NodeComplete(){
		super();
		type = Constants.Node_Complete;
	}
}