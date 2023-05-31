package odme.core;

/**
 * <h1>FlagVariables</h1>
 * <p>
 * This class contains those variables which are used for counting. For storing
 * these variables value as an object this FlagVariable class is used. As a
 * result when the saved project will be opened then the count will start from
 * the previously saved number.
 * </p>
 *
 * @author ---
 * @version ---
 */

public class FlagVariables implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	public int nodeNumber;
    public int uniformityNodeNumber;

}
